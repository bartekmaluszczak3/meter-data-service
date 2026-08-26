package org.example.gateway.service.domain.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class MeterEvent {
    private String eventId;
    private String meterId;
    private String eventType;
    private String occurredAt;
    private int version;
    private String createdAt;
    private String deviceType;
    private String gridZone;
    private Double voltage;
    private Double frequency;
    private Double activePower;
    private Double reactivePower;
    private String readingTimestamp;
    private String recordedAt;

}
