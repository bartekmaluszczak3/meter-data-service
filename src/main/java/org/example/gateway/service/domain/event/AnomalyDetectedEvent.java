package org.example.gateway.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectedEvent extends DomainEvent {
    private AnomalyType anomalyType;
    private String description;
    private Double detectedValue;
    private Double threshold;
    private Severity severity;

    @Override
    public EventType getEventType() {
        return EventType.ANOMALY_DETECTED;
    }
}