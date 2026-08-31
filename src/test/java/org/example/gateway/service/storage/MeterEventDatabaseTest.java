package org.example.gateway.service.storage;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.utils.IntegrationBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.gateway.domain.value.DeviceType.SOLAR_PANEL;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class MeterEventDatabaseTest extends IntegrationBaseTest {

    @BeforeEach
    void beforeEach(){
        clear();
    }

    @SneakyThrows
    @Test
    void shouldSaveEventInDatabase() {
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);

        kafkaTemplate.send(topic, meterId, payload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = countEvent();
                    assertThat(count).isEqualTo(1);
                });

        var event = getAllEventRows().get(0);
        String eventId = (String) event.get("meter_id");
        Assertions.assertEquals("meter-001", eventId);
        String eventType = (String) event.get("event_type");
        Assertions.assertEquals("METER_READING_RECORDED", eventType);
        JsonNode eventData = objectMapper.readTree(
                ((PGobject) event.get("event_data")).getValue());
        Assertions.assertEquals("meter-001", eventData.get("meterId").asText());
        Assertions.assertEquals(230.1, eventData.get("voltage").asDouble());
        Assertions.assertEquals(50.0, eventData.get("frequency").asDouble());
        Assertions.assertEquals("SMART_METER", eventData.get("deviceType").asText());
    }

    @SneakyThrows
    @Test
    void shouldSaveAnomalyEventInDatabase() {
        // given
        String topic = "telemetry.solar";
        String meterId = "solar-001";

        Readings readings = new Readings(
                300.0, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload("solar-001", SOLAR_PANEL, Instant.now(), readings);
        kafkaTemplate.send(topic, meterId, payload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = countAnomalies();
                    assertThat(count).isEqualTo(1);
                });

        var anomaly = getAllAnomalyRows().get(0);
        String eventId = (String) anomaly.get("meter_id");
        Assertions.assertEquals("solar-001", eventId);
        Assertions.assertEquals("VOLTAGE_OUT_OF_RANGE", anomaly.get("anomaly_type").toString());
    }
}
