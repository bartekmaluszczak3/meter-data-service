package org.example.gateway.service.domain.repository.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.repository.MeterQueryRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.DeviceType;
import org.example.gateway.service.web.dto.MeterReading;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
@Slf4j
public class MeterQueryRepositoryPostgres implements MeterQueryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MeterReading> meterReadingMapper = (rs, rowNum) ->
            new MeterReading(
                    rs.getString("meter_id"),
                    DeviceType.valueOf(rs.getString("device_type")),
                    rs.getString("grid_zone"),
                    rs.getTimestamp("reading_timestamp").toInstant(),
                    rs.getDouble("voltage"),
                    rs.getDouble("frequency"),
                    rs.getDouble("active_power"),
                    rs.getDouble("reactive_power"),
                    rs.getTimestamp("recorded_at").toInstant());

    @Override
    public List<MeterReading> getRecentReading(String meterId, int limit) throws DatabaseException {
        String sql = """
            SELECT meter_id, device_type, grid_zone, reading_timestamp,
                   voltage, frequency, active_power, reactive_power, recorded_at, created_at
            FROM meter_readings_materialized
            WHERE meter_id = ?
            ORDER BY reading_timestamp DESC
            LIMIT ?
            """;
        try {
            return jdbcTemplate.query(
                    sql,
                    ps -> {
                        ps.setString(1, meterId);
                        ps.setInt(2, limit);
                    },
                    meterReadingMapper
            );

        }catch (Exception e){
            log.error("Failed to query meter readings for meterId : {}", meterId);
            throw new DatabaseException("Failed to save event");
        }
    }

}
