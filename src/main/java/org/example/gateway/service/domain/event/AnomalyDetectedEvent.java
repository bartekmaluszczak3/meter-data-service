package org.example.gateway.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectedEvent extends DomainEvent {
    private String anomalyType;  // OVERVOLTAGE, UNDERCURRENT, FREQUENCY_DEVIATION
    private String description;
    private Double detectedValue;
    private Double threshold;
    private String severity;     // CRITICAL, WARNING, INFO

    @Override
    public String getEventType() {
        return "ANOMALY_DETECTED";
    }
}