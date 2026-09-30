package org.example.gateway.service.domain.repository.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.repository.ReportRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.DeviceType;
import org.example.gateway.service.web.dto.HourlyStatistics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
@Repository
@AllArgsConstructor
@Slf4j
public class ReportRepositoryPostgres implements ReportRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<HourlyStatistics> hourlyStatisticsRowMapper = (rs, rowNum) ->
            new HourlyStatistics(
                    rs.getTimestamp("hour").toInstant(),
                    rs.getString("meter_id"),
                    rs.getString("grid_zone"),
                    DeviceType.valueOf(rs.getString("device_type")),
                    rs.getInt("reading_count"),
                    rs.getDouble("avg_voltage"),
                    rs.getDouble("min_voltage"),
                    rs.getDouble("max_voltage"),
                    rs.getDouble("avg_frequency"),
                    rs.getDouble("avg_active_power"),
                    rs.getDouble("avg_reactive_power"));
    @Override
    public List<HourlyStatistics> getHourly(String meterId, Instant from, Instant to) throws DatabaseException {
        String sql = """
                SELECT hour, meter_id, device_type, grid_zone, reading_count,
                       avg_voltage, min_voltage, max_voltage, avg_frequency,
                       avg_active_power, avg_reactive_power
                FROM meter_readings_hourly
                WHERE meter_id = ?
                AND hour >= ?
                AND hour <= ?
                ORDER BY hour DESC
                """;

        try {
            return jdbcTemplate.query(
                    sql,
                    ps -> {
                        ps.setString(1, meterId);
                        ps.setTimestamp(2, Timestamp.from(from));
                        ps.setTimestamp(3, Timestamp.from(to));
                    },
                    hourlyStatisticsRowMapper
            );

        } catch (Exception e) {
            log.error("Failed to query meter readings for meterId : {} from: {} to {}", meterId, from, to);
            e.printStackTrace();
            throw new DatabaseException("Failed to query meter reading");
        }
    }
}
