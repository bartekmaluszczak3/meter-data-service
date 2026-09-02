package org.example.gateway.service.domain.repository.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.repository.AnomalyQueryRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
@Slf4j
@Repository
@AllArgsConstructor
public class AnomalyQueryRepositoryPostgres implements AnomalyQueryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Anomaly> anomalyReadingMapper = (rs, rowNum) ->
            new Anomaly(
                    rs.getString("meter_id"),
                    AnomalyType.valueOf(rs.getString("anomaly_type")),
                    rs.getString("description"),
                    rs.getTimestamp("detected_at").toInstant(),
                    rs.getDouble("detected_value"),
                    rs.getDouble("threshold"),
                    Severity.valueOf(rs.getString("severity")));

    @Override
    public List<Anomaly> getRecentAnomaly(String meterId, int limit) throws DatabaseException {
        String sql = """
            SELECT meter_id, detected_at, anomaly_type, description,
                   detected_value, threshold, severity
            FROM meter_anomalies
            WHERE meter_id = ?
            ORDER BY detected_at DESC
            LIMIT ?
            """;
        try {
            return jdbcTemplate.query(
                    sql,
                    ps -> {
                        ps.setString(1, meterId);
                        ps.setInt(2, limit);
                    },
                    anomalyReadingMapper
            );

        }catch (Exception e){
            log.error("Failed to query anomalies for meterId : {}", meterId);
            e.printStackTrace();
            throw new DatabaseException("Failed to query anomalies");
        }
    }

    @Override
    public List<Anomaly> getAnomaliesInRange(String meterId, Instant from, Instant to) throws DatabaseException {
        String sql = """
                SELECT meter_id, detected_at, anomaly_type, description,
                detected_value, threshold, severity
                FROM meter_anomalies
                WHERE meter_id = ?
                AND detected_at >= ?
                AND detected_at <= ?
                """;
        try {
            return jdbcTemplate.query(
                    sql,
                    ps -> {
                        ps.setString(1, meterId);
                        ps.setTimestamp(2, Timestamp.from(from));
                        ps.setTimestamp(3, Timestamp.from(to));                    },
                    anomalyReadingMapper
            );

        }catch (Exception e){
            log.error("Failed to query anomalies for meterId : {} from {} to {}", meterId, from, to);
            throw new DatabaseException("Failed to query anomalies");
        }
    }
}
