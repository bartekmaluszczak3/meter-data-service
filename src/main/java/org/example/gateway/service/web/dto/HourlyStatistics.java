package org.example.gateway.service.web.dto;

import java.time.Instant;

public record HourlyStatistics (
        Instant hour,
        String meterId,
        String gridZone,
        DeviceType deviceType,
        int readingCount,
        Double avgVoltage,
        Double minVoltage,
        Double maxVoltage,
        Double avgFrequency,
        Double avgActivePower,
        Double avgReactivePower
        ) {
}
