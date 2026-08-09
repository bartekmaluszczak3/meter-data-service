package org.example.gateway.service.domain.event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterDeactivatedEvent extends DomainEvent {
    private String reason;

    @Override
    public EventType getEventType() {
        return EventType.METER_DEACTIVATED;
    }
}
