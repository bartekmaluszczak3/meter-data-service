package org.example.gateway.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent implements Serializable {
    private String eventId = UUID.randomUUID().toString();
    private String meterId;
    private Instant occurredAt = Instant.now();
    private int eventVersion = 1;

    public abstract String getEventType();
}

