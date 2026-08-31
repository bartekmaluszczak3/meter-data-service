package org.example.gateway.service.domain.repository.projection;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
@AllArgsConstructor
@Slf4j
public class ProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(MeterReadingRecordedEvent event) throws DatabaseException {
        String sql = """
            INSERT INTO meter_readings_materialized
            (meter_id, device_type, grid_zone, reading_timestamp, voltage, frequency, 
             active_power, reactive_power, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (meter_id, reading_timestamp) DO NOTHING
            """;
        try {
            int inserted = jdbcTemplate.update(sql,
                    event.getMeterId(),
                    event.getDeviceType(),
                    event.getGridZone(),
                    Timestamp.from(event.getReadingTimestamp()),
                    event.getVoltage(),
                    event.getFrequency(),
                    event.getActivePower(),
                    event.getReactivePower(),
                    Timestamp.from(event.getRecordedAt())
            );
            if (inserted > 0) {
                log.debug("Reading projected: meter={}, voltage={}",
                        event.getMeterId(), event.getVoltage());
            }

        } catch (Exception e) {
            log.error("Failed to project reading for meter: {}", event.getMeterId(), e);
            throw new DatabaseException("Failed to save event");        }
    }
}
