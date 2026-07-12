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
    public String getEventType() {
        return "METER_DEACTIVATED";
    }
}
