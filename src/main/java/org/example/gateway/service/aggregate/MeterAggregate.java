package org.example.gateway.service.aggregate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.gateway.service.domain.event.*;

import java.time.Instant;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterAggregate {
    private String meterId;
    private String deviceType;
    private String gridZone;
    private boolean active;
    private Instant activatedAt;
    private Instant deactivatedAt;

    private List<MeterReading> readings = new ArrayList<>();
    private List<DetectedAnomaly> anomalies = new ArrayList<>();
    private int eventVersion = 0;

    public void rebuildFromEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            applyEvent(event);
        }
    }
    private void applyEvent(DomainEvent event) {
        if (event instanceof MeterReadingRecordedEvent) {
            applyMeterReadingRecorded((MeterReadingRecordedEvent) event);
        } else if (event instanceof AnomalyDetectedEvent) {
            applyAnomalyDetected((AnomalyDetectedEvent) event);
        } else if (event instanceof MeterActivatedEvent) {
            applyMeterActivated((MeterActivatedEvent) event);
        } else if (event instanceof MeterDeactivatedEvent) {
            applyMeterDeactivated((MeterDeactivatedEvent) event);
        }
        this.eventVersion = event.getEventVersion();
    }

    private void applyMeterReadingRecorded(MeterReadingRecordedEvent event) {
        this.meterId = event.getMeterId();
        this.deviceType = event.getDeviceType();
        this.gridZone = event.getGridZone();

        MeterReading reading = new MeterReading(
                event.getVoltage(),
                event.getFrequency(),
                event.getActivePower(),
                event.getReactivePower(),
                event.getReadingTimestamp(),
                event.getRecordedAt(),
                false
        );
        this.readings.add(reading);
    }

    private void applyAnomalyDetected(AnomalyDetectedEvent event) {
        DetectedAnomaly anomaly = new DetectedAnomaly(
                event.getAnomalyType(),
                event.getDescription(),
                event.getDetectedValue(),
                event.getThreshold(),
                event.getSeverity(),
                event.getOccurredAt()
        );
        this.anomalies.add(anomaly);

        if (!this.readings.isEmpty()) {
            this.readings.get(this.readings.size() - 1).setAnomaly(true);
        }
    }

    private void applyMeterActivated(MeterActivatedEvent event) {
        this.meterId = event.getMeterId();
        this.deviceType = event.getDeviceType();
        this.gridZone = event.getGridZone();
        this.active = true;
        this.activatedAt = event.getOccurredAt();
    }

    private void applyMeterDeactivated(MeterDeactivatedEvent event) {
        this.active = false;
        this.deactivatedAt = event.getOccurredAt();
    }

    public List<MeterReading> getLastReadings(int count) {
        int size = this.readings.size();
        if (size <= count) {
            return new ArrayList<>(this.readings);
        }
        return new ArrayList<>(this.readings.subList(size - count, size));
    }
    public double getAveragePower(Instant from, Instant to) {
        return this.readings.stream()
                .filter(r -> r.getReadingTimestamp().isAfter(from) &&
                        r.getReadingTimestamp().isBefore(to))
                .mapToDouble(MeterReading::getActivePower)
                .average()
                .orElse(0.0);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeterReading {
        private Double voltage;
        private Double frequency;
        private Double activePower;
        private Double reactivePower;
        private Instant readingTimestamp;
        private Instant recordedAt;
        private boolean anomaly;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedAnomaly {
        private String type;           // OVERVOLTAGE, UNDERCURRENT, etc
        private String description;
        private Double detectedValue;
        private Double threshold;
        private String severity;       // CRITICAL, WARNING, INFO
        private Instant detectedAt;
    }
}