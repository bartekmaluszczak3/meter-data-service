package org.example.gateway.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterActivatedEvent extends DomainEvent {
    private String deviceType;
    private String gridZone;

    @Override
    public String getEventType() {
        return "METER_ACTIVATED";
    }
}
