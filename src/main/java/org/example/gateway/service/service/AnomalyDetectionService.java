package org.example.gateway.service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.service.config.AnomaliesRangeProperties;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AnomalyDetectionService {
    private final AnomaliesRangeProperties anomaliesRangeProperties;


    public List<AnomalyDetectedEvent> detectAnomalies(TelemetryPayload payload) {
        List<AnomalyDetectedEvent> anomalies = new ArrayList<>();

        if (payload.getReadings() == null) {
            return anomalies;
        }

        switch (payload.getDeviceType()) {
            case SMART_METER -> anomalies.addAll(detectSmartMeterAnomalies(payload));
            case SOLAR_PANEL -> anomalies.addAll(detectSolarPanelAnomalies(payload));
            case WIND_TURBINE -> anomalies.addAll(detectWindTurbineAnomalies(payload));
            case EV_CHARGER -> anomalies.addAll(detectEVChargerAnomalies(payload));
            case BATTERY_STORAGE -> anomalies.addAll(detectBatteryStorageAnomalies(payload));
        }

        return anomalies;
    }

    private List<AnomalyDetectedEvent> detectSmartMeterAnomalies(TelemetryPayload payload) {
        List<AnomalyDetectedEvent> anomalies = new ArrayList<>();
        var properties = anomaliesRangeProperties.getSmartMeter();

        if (payload.getReadings().getVoltage() < properties.getVoltage().getMin() ||
                payload.getReadings().getVoltage() > properties.getVoltage().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "OVERVOLTAGE",
                    "Voltage out of range: " + payload.getReadings().getVoltage() + "V",
                    payload.getReadings().getVoltage(),
                    230.0,
                    "WARNING"
            ));
        }

        if (payload.getReadings().getFrequency() < properties.getFrequency().getMin() ||
                payload.getReadings().getFrequency() > properties.getFrequency().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "FREQUENCY_DEVIATION",
                    "Frequency out of range: " + payload.getReadings().getFrequency() + "Hz",
                    payload.getReadings().getFrequency(),
                    50.0,
                    "WARNING"
            ));
        }

        // Reactive power should be low
        if (payload.getReadings().getReactivePower() > properties.getReactivePower().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "HIGH_REACTIVE_POWER",
                    "Reactive power too high: " + payload.getReadings().getReactivePower(),
                    payload.getReadings().getReactivePower(),
                    5.0,
                    "INFO"
            ));
        }

        return anomalies;
    }

    private List<AnomalyDetectedEvent> detectSolarPanelAnomalies(TelemetryPayload payload) {
        var properties = anomaliesRangeProperties.getSolarPanel();
        List<AnomalyDetectedEvent> anomalies = new ArrayList<>();

        if (payload.getReadings().getVoltage() < properties.getVoltage().getMin() ||
                payload.getReadings().getVoltage() >  properties.getVoltage().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "VOLTAGE_OUT_OF_RANGE",
                    "Solar panel voltage: " + payload.getReadings().getVoltage(),
                    payload.getReadings().getVoltage(),
                    230.0,
                    "WARNING"
            ));
        }

        return anomalies;
    }

    private List<AnomalyDetectedEvent> detectWindTurbineAnomalies(TelemetryPayload payload) {
        List<AnomalyDetectedEvent> anomalies = new ArrayList<>();
        var properties = anomaliesRangeProperties.getWindTurbine();

        if (payload.getReadings().getFrequency() < properties.getFrequency().getMin() ||
                payload.getReadings().getFrequency() >  properties.getFrequency().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "FREQUENCY_DEVIATION",
                    "Wind turbine frequency: " + payload.getReadings().getFrequency(),
                    payload.getReadings().getFrequency(),
                    50.0,
                    "WARNING"
            ));
        }

        return anomalies;
    }

    private List<AnomalyDetectedEvent> detectEVChargerAnomalies(TelemetryPayload payload) {
        List<AnomalyDetectedEvent> anomalies = new ArrayList<>();
        var properties = anomaliesRangeProperties.getEvCharger();

        if (payload.getReadings().getActivePower() > properties.getActivePower().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "OVERCURRENT",
                    "EV Charger power exceeds limit: " + payload.getReadings().getActivePower() + "kW",
                    payload.getReadings().getActivePower(),
                    11.5,
                    "CRITICAL"
            ));
        }

        return anomalies;
    }

    private List<AnomalyDetectedEvent> detectBatteryStorageAnomalies(TelemetryPayload payload) {

        List<AnomalyDetectedEvent> anomalies = new ArrayList<>();
        var properties = anomaliesRangeProperties.getBattery();

        if (payload.getReadings().getFrequency() < properties.getFrequency().getMin() ||
                payload.getReadings().getFrequency() > properties.getFrequency().getMax()) {
            anomalies.add(createAnomalyEvent(
                    payload,
                    "FREQUENCY_INSTABILITY",
                    "Battery storage frequency critical: " + payload.getReadings().getFrequency(),
                    payload.getReadings().getFrequency(),
                    50.0,
                    "CRITICAL"
            ));
        }

        return anomalies;
    }

    private AnomalyDetectedEvent createAnomalyEvent(
            TelemetryPayload payload,
            String anomalyType,
            String description,
            Double detectedValue,
            Double threshold,
            String severity) {

        AnomalyDetectedEvent event = new AnomalyDetectedEvent();
        event.setMeterId(payload.getDeviceId());
        event.setAnomalyType(anomalyType);
        event.setDescription(description);
        event.setDetectedValue(detectedValue);
        event.setThreshold(threshold);
        event.setSeverity(severity);
        event.setOccurredAt(Instant.now());
        event.setEventVersion(1);

        return event;
    }
}
