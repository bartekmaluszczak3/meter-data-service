package org.example.gateway.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingRecordedEvent extends DomainEvent {
    private String deviceType;
    private String gridZone;
    private Double voltage;
    private Double frequency;
    private Double activePower;
    private Double reactivePower;
    private Instant readingTimestamp;
    private Instant recordedAt;

    @Override
    public String getEventType() {
        return "METER_READING_RECORDED";
    }
}