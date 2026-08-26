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
import java.util.Map;

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
                    event.getEventType().name(),
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

    public void save(AnomalyDetectedEvent anomalyDetectedEvent, String eventId, Instant eventOccurredAt)  throws DatabaseException{
        String sql = """
            INSERT INTO meter_anomalies
            (event_id, event_occurred_at, meter_id, anomaly_type, description,
             detected_value, threshold, severity, detected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try{
            jdbcTemplate.update(sql,
                    eventId,
                    Timestamp.from(eventOccurredAt),
                    anomalyDetectedEvent.getMeterId(),
                    anomalyDetectedEvent.getAnomalyType().name(),
                    anomalyDetectedEvent.getDescription(),
                    anomalyDetectedEvent.getDetectedValue(),
                    anomalyDetectedEvent.getThreshold(),
                    anomalyDetectedEvent.getSeverity().name(),
                    Timestamp.from(anomalyDetectedEvent.getOccurredAt())
            );
            log.debug("Anomaly appended: {} ", anomalyDetectedEvent.getEventId());
        } catch (Exception e) {
            log.error("Failed to save Anomaly");
            e.printStackTrace();
            throw new DatabaseException("Failed to save Anomaly");
        }
    }

    public List<Map<String, Object>> getMeterEvents() {
    String sql = """
        SELECT *
        FROM meter_events
        """;
        return jdbcTemplate.queryForList(sql);
    }


    public List<Map<String, Object>> getAnomalies() {
        String sql = """
        SELECT *
        FROM meter_anomalies
        """;
        return jdbcTemplate.queryForList(sql);
    }
}
