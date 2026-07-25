package org.example.gateway.service.service;

import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.Application;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.example.gateway.domain.value.DeviceType.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class AnomalyDetectionServiceTest {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Test
    void shouldDetectSmartMeterAnomaly() {
        // given
        Readings readings = new Readings(
                150.1, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload(UUID.randomUUID().toString(), SMART_METER, Instant.now(), readings);

        // when
        var anomalies = anomalyDetectionService.detectAnomalies(payload);

        // then
        Assertions.assertEquals(3, anomalies.size());
        assertAnomalyType(anomalies, "OVERVOLTAGE");
        assertAnomalyType(anomalies, "FREQUENCY_DEVIATION");
        assertAnomalyType(anomalies, "HIGH_REACTIVE_POWER");
    }

    @Test
    void shouldDetectSolarPanelAnomaly() {
        // given
        Readings readings = new Readings(
                300.0, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload(UUID.randomUUID().toString(), SOLAR_PANEL, Instant.now(), readings);

        // when
        var anomalies = anomalyDetectionService.detectAnomalies(payload);

        // then
        Assertions.assertEquals(1, anomalies.size());
        assertAnomalyType(anomalies, "VOLTAGE_OUT_OF_RANGE");
    }

    @Test
    void shouldDetectWindTurbineAnomaly() {
        // given
        Readings readings = new Readings(
                300.0, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload(UUID.randomUUID().toString(), WIND_TURBINE, Instant.now(), readings);

        // when
        var anomalies = anomalyDetectionService.detectAnomalies(payload);

        // then
        Assertions.assertEquals(1, anomalies.size());
        assertAnomalyType(anomalies, "FREQUENCY_DEVIATION");
    }

    @Test
    void shouldDetectEVChargerAnomaly() {
        // given
        Readings readings = new Readings(
                300.0, 5.0, 20.0, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload(UUID.randomUUID().toString(), EV_CHARGER, Instant.now(), readings);

        // when
        var anomalies = anomalyDetectionService.detectAnomalies(payload);

        // then
        Assertions.assertEquals(1, anomalies.size());
        assertAnomalyType(anomalies, "OVERCURRENT");
    }
    @Test
    void shouldDetectBatteryAnomaly() {
        // given
        Readings readings = new Readings(
                300.0, 5.0, 20.0, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload(UUID.randomUUID().toString(), BATTERY_STORAGE, Instant.now(), readings);

        // when
        var anomalies = anomalyDetectionService.detectAnomalies(payload);

        // then
        Assertions.assertEquals(1, anomalies.size());
        assertAnomalyType(anomalies, "FREQUENCY_INSTABILITY");
    }

    private void assertAnomalyType(List<AnomalyDetectedEvent> anomalies, String type) {
        Optional<AnomalyDetectedEvent> anomalyDetected = anomalies.stream().filter(e -> e.getAnomalyType().equals(type)).findAny();
        Assertions.assertTrue(anomalyDetected.isPresent());
    }

}
