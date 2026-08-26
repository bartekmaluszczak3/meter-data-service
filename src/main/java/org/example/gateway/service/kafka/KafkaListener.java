package org.example.gateway.service.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.service.AnomalyDetectionService;
import org.example.gateway.service.service.MeterEventService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class KafkaListener {
    private final MeterEventService meterEventService;
    private final AnomalyDetectionService anomalyDetectionService;

    @org.springframework.kafka.annotation.KafkaListener(topics = {
            "telemetry.meters",
            "telemetry.solar",
            "telemetry.wind",
            "telemetry.ev",
            "telemetry.battery"
    }, groupId = "meter-data-service")

    public void processTelemetry(TelemetryPayload payload) {
        log.info("Processing telemetry for meter: {} ({})",
                payload.getDeviceId(), payload.getDeviceType());
        try {
            MeterReadingRecordedEvent readingEvent = createReadingEvent(payload);
            meterEventService.save(readingEvent);

            List<AnomalyDetectedEvent> anomalies =
                    anomalyDetectionService.detectAnomalies(payload);

            for (AnomalyDetectedEvent anomaly : anomalies) {
                meterEventService.save(anomaly, readingEvent.getEventId(), readingEvent.getRecordedAt());
                log.warn("Anomaly detected for meter {}: {}",
                        payload.getDeviceId(), anomaly.getAnomalyType());
            }
        }catch (Exception e) {
            log.error("Error processing telemetry for meter: {}",
                    payload.getDeviceId(), e);
            throw new RuntimeException("Failed to process telemetry", e);
        }
    }

    private MeterReadingRecordedEvent createReadingEvent(TelemetryPayload payload) {
        MeterReadingRecordedEvent event = new MeterReadingRecordedEvent();
        event.setMeterId(payload.getDeviceId());
        event.setDeviceType(payload.getDeviceType().name());
        event.setGridZone(payload.getGridZone());
        event.setVoltage(payload.getReadings().getVoltage());
        event.setFrequency(payload.getReadings().getFrequency());
        event.setActivePower(payload.getReadings().getActivePower());
        event.setReactivePower(payload.getReadings().getReactivePower());
        event.setReadingTimestamp(payload.getTimestamp());
        event.setRecordedAt(payload.getReceivedAt());
        event.setOccurredAt(Instant.now());
        event.setEventVersion(1);
        return event;
    }
}

