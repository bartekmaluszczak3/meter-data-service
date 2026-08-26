package org.example.gateway.service.domain.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class Anomaly {
        private String anomalyId;
        private String meterId;
        private String eventId;
        private String eventOccurredAt;
        private String anomalyType;
        private String description;
        private String detectedValue;
        private String threshold;
        private String severity;
        private String detectedAt;
        private String createdAt;

}
