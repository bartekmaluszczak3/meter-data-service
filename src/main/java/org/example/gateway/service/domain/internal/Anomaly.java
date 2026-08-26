package org.example.gateway.service.domain.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.example.gateway.service.domain.event.AnomalyType;
import org.example.gateway.service.domain.event.Severity;

@AllArgsConstructor
@Builder
@Data
public class Anomaly {
        private String anomalyId;
        private String meterId;
        private String eventId;
        private String eventOccurredAt;
        private AnomalyType anomalyType;
        private String description;
        private Double detectedValue;
        private Double threshold;
        private Severity severity;
        private String detectedAt;
        private String createdAt;

}
