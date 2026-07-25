package org.example.gateway.service.domain.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.event.*;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class MeterEventRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void save(DomainEvent event) throws DatabaseException {
        String sql = """
        INSERT INTO meter_events 
        (event_id, meter_id, event_type, event_data, occurred_at, version)
        VALUES (?, ?, ?, ?::jsonb, ?, ?)
        """;
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            jdbcTemplate.update(sql,
                    event.getEventId(),
                    event.getMeterId(),
                    event.getEventType(),
                    eventJson,
                    Timestamp.from(event.getOccurredAt()),
                    event.getEventVersion()
            );

            log.debug("Event appended: {} ({})", event.getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to save event");
            throw new DatabaseException("Failed to save event");
        }
    }

    public List<DomainEvent> getEventStream(String meterId) throws DatabaseException {
        String sql = """
            SELECT event_data, event_type
            FROM meter_events
            WHERE meter_id = ?
            ORDER BY occurred_at ASC
            """;

        try {
            return jdbcTemplate.query(sql, new Object[]{meterId}, (rs, rowNum) -> {
                String eventJson = rs.getString("event_data");
                String eventType = rs.getString("event_type");

                return deserializeEvent(eventJson, eventType);
            });
        } catch (Exception e) {
            log.error("Failed to retrieve event stream for meter: {}", meterId, e);
            throw new DatabaseException("Failed to retrieve event");
        }
    }
    public List<DomainEvent> getEventStream(String meterId, Instant from, Instant to) throws DatabaseException {
        String sql = """
            SELECT event_data, event_type
            FROM meter_events
            WHERE meter_id = ?
            AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp ASC
            """;

        try {
            return jdbcTemplate.query(sql,
                    new Object[]{meterId, Timestamp.from(from), Timestamp.from(to)},
                    (rs, rowNum) -> {
                        String eventJson = rs.getString("event_data");
                        String eventType = rs.getString("event_type");

                        return deserializeEvent(eventJson, eventType);
                    });
        } catch (Exception e) {
            log.error("Failed to retrieve event stream for meter: {}", meterId, e);
            throw new DatabaseException("Failed to retrieve event");
        }
    }

    private DomainEvent deserializeEvent(String json, String eventType) {
        try {
            return switch (eventType) {
                case "METER_READING_RECORDED" ->
                        objectMapper.readValue(json, MeterReadingRecordedEvent.class);
                case "ANOMALY_DETECTED" ->
                        objectMapper.readValue(json, AnomalyDetectedEvent.class);
                case "METER_ACTIVATED" ->
                        objectMapper.readValue(json, MeterActivatedEvent.class);
                case "METER_DEACTIVATED" ->
                        objectMapper.readValue(json, MeterDeactivatedEvent.class);
                default -> throw new RuntimeException("Unknown event type: " + eventType);
            };
        } catch (Exception e) {
            log.error("Failed to deserialize event: {}", eventType, e);
            throw new RuntimeException("Event deserialization error", e);
        }
    }
}
