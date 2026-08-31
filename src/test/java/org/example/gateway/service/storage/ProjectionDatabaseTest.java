package org.example.gateway.service.storage;

import org.assertj.core.api.Assertions;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.utils.IntegrationBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class ProjectionDatabaseTest extends IntegrationBaseTest {

    @BeforeEach
    void beforeEach() {
        clear();
    }

    @Test
    void shouldSaveProjectionInDataBase() {
        // given
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);

        kafkaTemplate.send(topic, meterId, payload);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = countProjection();
                    assertThat(count).isEqualTo(1);
                });

        var projection = getAllProjection().get(0);

        org.junit.jupiter.api.Assertions.assertEquals("SMART_METER", projection.get("device_type"));
        org.junit.jupiter.api.Assertions.assertEquals("230.10", projection.get("voltage").toString());
        org.junit.jupiter.api.Assertions.assertEquals("50.00", projection.get("frequency").toString());
        org.junit.jupiter.api.Assertions.assertEquals("3.45", projection.get("active_power").toString());
        org.junit.jupiter.api.Assertions.assertEquals("0.50", projection.get("reactive_power").toString());

    }
}
