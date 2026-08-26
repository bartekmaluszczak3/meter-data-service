package org.example.gateway.service.agregate;

import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.agragate.MeterAggregator;
import org.example.gateway.service.domain.event.Severity;
import org.example.gateway.service.utils.IntegrationBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.gateway.domain.value.DeviceType.SOLAR_PANEL;
import static org.example.gateway.service.domain.event.AnomalyType.VOLTAGE_OUT_OF_RANGE;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class MeterAggregatorTest  extends IntegrationBaseTest {

    @Autowired
    MeterAggregator meterAggregator;

    @BeforeEach
    void beforeEach(){
        clear();
    }

    @SneakyThrows
    @Test
    void shouldAggregateEvents() {
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);

        kafkaTemplate.send(topic, meterId, payload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = count();
                    assertThat(count).isEqualTo(1);
                });

        // when
        var aggregateEvent = meterAggregator.getMeterEvents();

        // then
        Assertions.assertEquals(1, aggregateEvent.size());
        var data = aggregateEvent.get(0);
        Assertions.assertEquals("meter-001", data.getMeterId());
        Assertions.assertEquals(230.1, data.getVoltage());
        Assertions.assertEquals(50.0, data.getFrequency());
        Assertions.assertEquals(3.45, data.getActivePower());
        Assertions.assertEquals(0.5, data.getReactivePower());

    }

    @SneakyThrows
    @Test
    void shouldAggregateAnomalies() {
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        Readings readings = new Readings(
                300.0, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload("meter-001", SOLAR_PANEL, Instant.now(), readings);

        kafkaTemplate.send(topic, meterId, payload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = count();
                    assertThat(count).isEqualTo(1);
                });

        // when
        var aggregateEvent = meterAggregator.getAnomalies();

        // then
        Assertions.assertEquals(1, aggregateEvent.size());
        var data = aggregateEvent.get(0);
        Assertions.assertEquals("meter-001", data.getMeterId());
        Assertions.assertEquals(VOLTAGE_OUT_OF_RANGE, data.getAnomalyType());
        Assertions.assertTrue(data.getDescription().contains("Solar panel voltage"));
        Assertions.assertEquals(300.0, data.getDetectedValue());
        Assertions.assertEquals(Severity.WARNING, data.getSeverity());
    }
}
