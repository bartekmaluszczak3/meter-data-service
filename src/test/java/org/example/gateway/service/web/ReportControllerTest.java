package org.example.gateway.service.web;

import org.example.gateway.service.utils.IntegrationBaseTest;
import org.example.gateway.service.web.dto.DeviceType;
import org.example.gateway.service.web.dto.HourlyStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportControllerTest extends IntegrationBaseTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void beforeEach() {
        clear();
    }


    @Test
    void shouldReturnHourlyStats() {
        // given
        Instant now = Instant.now();
        createReading(DeviceType.WIND_TURBINE, now.minus(2, ChronoUnit.MINUTES), 300.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(12, ChronoUnit.MINUTES), 100.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(10, ChronoUnit.MINUTES), 200.0);
        flush();
        String url = "/api/v1/reports/meters/device-0001/daily";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<HourlyStatistics>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(1, body.size());
        var stat = body.get(0);
        Assertions.assertEquals(3, stat.readingCount());
        Assertions.assertEquals(200.0, stat.avgVoltage());
    }

    @Test
    void shouldReturnStatsFromMultipleHours() {
        // given
        Instant now = Instant.now();
        createReading(DeviceType.WIND_TURBINE, now.minus(2, ChronoUnit.MINUTES), 300.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(12, ChronoUnit.MINUTES), 100.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(62, ChronoUnit.MINUTES), 200.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(72, ChronoUnit.MINUTES), 150.0);

        flush();
        String url = "/api/v1/reports/meters/device-0001/daily";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<HourlyStatistics>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(2, body.size());
        body.forEach(e->{
            Assertions.assertEquals(2, e.readingCount());
        });
    }

    @Test
    void shouldReturnWeeklyStats() {
        // given
        Instant now = Instant.now();
        createReading(DeviceType.WIND_TURBINE, now.minus(2, ChronoUnit.MINUTES), 300.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(12, ChronoUnit.MINUTES), 100.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(10, ChronoUnit.MINUTES), 200.0);
        flush();
        String url = "/api/v1/reports/meters/device-0001/weekly";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<HourlyStatistics>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(1, body.size());

        var stat = body.get(0);
        Assertions.assertEquals(3, stat.readingCount());
        Assertions.assertEquals(200.0, stat.avgVoltage());
    }

    @Test
    void shouldReturnStatsFromMultipleDays() {
        // given
        Instant now = Instant.now();
        createReading(DeviceType.WIND_TURBINE, now.minus(2, ChronoUnit.DAYS), 300.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(1, ChronoUnit.DAYS), 100.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(5, ChronoUnit.DAYS), 200.0);
        createReading(DeviceType.WIND_TURBINE, now.minus(4, ChronoUnit.DAYS), 150.0);

        flush();
        String url = "/api/v1/reports/meters/device-0001/weekly";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<HourlyStatistics>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(4, body.size());

    }


    private void createReading(DeviceType deviceType, Instant readingTimestamp, Double voltage){
        String sql = """
                         INSERT INTO meter_readings_materialized ( meter_id, reading_timestamp, device_type, grid_zone,
                         voltage, frequency, active_power, reactive_power, recorded_at )
                         VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, NOW() )""";
        jdbcTemplate.update(sql, "device-0001", Timestamp.from(readingTimestamp), deviceType.name(), "gridZone", voltage, 50.01, 12.40,  3.10);
    }

    private void flush() {
        jdbcTemplate.update("""
    CALL refresh_continuous_aggregate(
        'meter_readings_hourly',
        NULL,
        NULL
    )""");
    }
}

