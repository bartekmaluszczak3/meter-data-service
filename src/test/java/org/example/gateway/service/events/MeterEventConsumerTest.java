package org.example.gateway.service.events;

import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.aggregate.MeterAggregate;
import org.example.gateway.service.utils.IntegrationBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.gateway.domain.value.DeviceType.SMART_METER;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class MeterEventConsumerTest extends IntegrationBaseTest {

    @SneakyThrows
    @Test
    void shouldProperlyReceiveEventAndSaveToDatabase(){
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);

        // when
        kafkaTemplate.send(topic, meterId, payload);

        // then
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    MeterAggregate meterState = meterEventService.getMeterState(meterId);
                    assertThat(meterState.getMeterId()).isEqualTo(meterId);
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
                    List<String> metersId = List.of("solar-001","wind-001", "ev-001", "battery-001" );
                    for(String meter: metersId) {
                        MeterAggregate meterState = meterEventService.getMeterState(meter);
                        assertThat(meterState.getMeterId()).isEqualTo(meter);
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
                    MeterAggregate meterState = meterEventService.getMeterState(meterId);
                    var anomalies = meterState.getAnomalies();
                    Assertions.assertEquals(3, anomalies.size());
                });
    }
}
