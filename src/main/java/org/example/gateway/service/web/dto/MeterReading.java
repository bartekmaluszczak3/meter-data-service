package org.example.gateway.service.web.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record MeterReading(
        String meterId,
        DeviceType deviceType,
        String gridZone,
        Instant timestamp,
        Double voltage,
        Double frequency,
        Double activePower,
        Double reactivePower,
        Instant recordedAt
) {
}
