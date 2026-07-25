package org.example.gateway.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectedEvent extends DomainEvent {
    private String anomalyType;
    private String description;
    private Double detectedValue;
    private Double threshold;
    private String severity;

    @Override
    public String getEventType() {
        return "ANOMALY_DETECTED";
    }
}