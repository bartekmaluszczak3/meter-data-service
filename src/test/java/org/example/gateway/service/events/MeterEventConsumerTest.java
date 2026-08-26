package org.example.gateway.service.events;

import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.agragate.MeterAggregator;
import org.example.gateway.service.domain.internal.Anomaly;
import org.example.gateway.service.domain.internal.MeterEvent;
import org.example.gateway.service.utils.IntegrationBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.gateway.domain.value.DeviceType.SMART_METER;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class MeterEventConsumerTest extends IntegrationBaseTest {

    @Autowired
    MeterAggregator meterAggregator;

    @BeforeEach
    void beforeEach(){
        clear();
    }


    @SneakyThrows
    @Test
    void shouldProperlyReceiveEventAndSaveToDatabase(){
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        TelemetryPayload payload = buildPayload("meter-001", SMART_METER);

        // when
        kafkaTemplate.send(topic, meterId, payload);

        // then
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var meterEvent = meterAggregator.getMeterEvents();
                    assertThat(meterEvent.size()).isEqualTo(1);
                });
    }

    @SneakyThrows
    @Test
    void shouldPublishToDifferentTopic(){
        // when
        kafkaTemplate.send("telemetry.solar", "solar-001", buildPayload("solar-001",  DeviceType.SOLAR_PANEL));
        kafkaTemplate.send("telemetry.wind", "wind-001", buildPayload("wind-001",  DeviceType.WIND_TURBINE));
        kafkaTemplate.send("telemetry.ev", "ev-001", buildPayload("ev-001",  DeviceType.EV_CHARGER));
        kafkaTemplate.send("telemetry.battery", "battery-001", buildPayload("battery-001",  DeviceType.BATTERY_STORAGE));

        // then
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<MeterEvent> events = meterAggregator.getMeterEvents();
                    Assertions.assertEquals(events.size(), 4);
                    List<String> metersId = List.of("solar-001","wind-001", "ev-001", "battery-001" );
                    for(String meter: metersId) {
                        Optional<String> any = events.stream().map(MeterEvent::getMeterId).filter(e -> e.equals(meter)).findAny();
                        Assertions.assertTrue(any.isPresent());
                    }
                });
    }

    @Test
    void shouldDetectAnomalyInMeter() {
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        Readings readings = new Readings(
                150.1, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null);
        var payload = new TelemetryPayload(meterId, SMART_METER, Instant.now(), readings);

        // when
        kafkaTemplate.send(topic, meterId, payload);

        // then
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Anomaly> anomalies = meterAggregator.getAnomalies();
                    Assertions.assertEquals(3, anomalies.size());
                });
    }
}
