package org.example.gateway.service.web.dto;


import java.time.Instant;

public record Anomaly(
        String meterId,
        AnomalyType anomalyType,
        String description,
        Instant detectedAt,
        Double detectedValue,
        Double threshold,
        Severity severity
        )
{ }