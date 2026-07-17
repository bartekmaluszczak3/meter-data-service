package org.example.gateway.service.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.service.aggregate.MeterAggregate;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.example.gateway.service.domain.event.DomainEvent;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class MeterDataService {

    private final MeterEventRepository eventRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    @KafkaListener(topics = {
            "telemetry.meters",
            "telemetry.solar",
            "telemetry.wind",
            "telemetry.ev",
            "telemetry.battery"
    }, groupId = "meter-data-service")
    @Transactional
    public void processTelemetry(TelemetryPayload payload) {
        log.info("Processing telemetry for meter: {} ({})",
                payload.getDeviceId(), payload.getDeviceType());

        try {
            // Step 1: Create MeterReadingRecordedEvent
            MeterReadingRecordedEvent readingEvent = createReadingEvent(payload);

            // Step 2: Append to Event Store
            eventRepository.save(readingEvent);
            log.debug("Event appended: {}", readingEvent.getEventId());

            // Step 3: Detect anomalies
            List<AnomalyDetectedEvent> anomalies =
                    anomalyDetectionService.detectAnomalies(payload);

            // Step 4: Append anomaly events
            for (AnomalyDetectedEvent anomaly : anomalies) {
                eventRepository.save(anomaly);
                log.warn("Anomaly detected for meter {}: {}",
                        payload.getDeviceId(), anomaly.getAnomalyType());
            }

        } catch (Exception e) {
            log.error("Error processing telemetry for meter: {}",
                    payload.getDeviceId(), e);
            throw new RuntimeException("Failed to process telemetry", e);
        }
    }

    /**
     * Reconstruct meter state from event stream.
     * Used for CQRS read model or testing.
     */
    @Transactional(readOnly = true)
    public MeterAggregate getMeterState(String meterId) throws DatabaseException {
        List<DomainEvent> events = eventRepository.getEventStream(meterId);

        MeterAggregate meter = new MeterAggregate();
        meter.rebuildFromEvents(events);

        return meter;
    }


    @Transactional(readOnly = true)
    public List<MeterAggregate.MeterReading> getRecentReadings(String meterId, int count) throws DatabaseException {
        MeterAggregate meter = getMeterState(meterId);
        return meter.getLastReadings(count);
    }

    /**
     * Get anomalies detected for a meter in time window
     */
    @Transactional(readOnly = true)
    public List<MeterAggregate.DetectedAnomaly> getAnomalies(
            String meterId, Instant from, Instant to) throws DatabaseException {

        List<DomainEvent> events = eventRepository.getEventStream(meterId);
        MeterAggregate meter = new MeterAggregate();
        meter.rebuildFromEvents(events);

        return meter.getAnomalies().stream()
                .filter(a -> a.getDetectedAt().isAfter(from) &&
                        a.getDetectedAt().isBefore(to))
                .toList();
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
