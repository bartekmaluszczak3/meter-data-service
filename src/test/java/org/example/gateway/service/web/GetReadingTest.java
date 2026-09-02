package org.example.gateway.service.web;

import org.example.gateway.service.utils.IntegrationBaseTest;
import org.example.gateway.service.web.dto.DeviceType;
import org.example.gateway.service.web.dto.MeterReading;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.List;

public class GetReadingTest extends IntegrationBaseTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void beforeEach() {
        clear();
    }

    @Test
    void shouldReturnReading(){
        // given
        createReading(DeviceType.WIND_TURBINE);
        String url = "/api/v1/meters/device-0001/recent";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<MeterReading>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(1, body.size());
        var meter = body.get(0);
        Assertions.assertEquals("device-0001", meter.meterId());
        Assertions.assertEquals(DeviceType.WIND_TURBINE, meter.deviceType());
        Assertions.assertEquals("gridZone", meter.gridZone());
        Assertions.assertEquals(230.01, meter.voltage());
        Assertions.assertEquals(50.01, meter.frequency());
        Assertions.assertEquals(12.4, meter.activePower());
        Assertions.assertEquals(3.1, meter.reactivePower());
        Instant now = Instant.now();
        Assertions.assertTrue(now.isAfter(meter.recordedAt()));
    }

    @Test
    void shouldLimitTheReadings() {
        createReading(DeviceType.WIND_TURBINE);
        createReading(DeviceType.SMART_METER);
        createReading(DeviceType.SOLAR_PANEL);
        createReading(DeviceType.BATTERY_STORAGE);

        String url = "/api/v1/meters/device-0001/recent?limit=2";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<MeterReading>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(2, body.size());
    }

    private void createReading(DeviceType deviceType){
        String sql = """
                         INSERT INTO meter_readings_materialized ( meter_id, reading_timestamp, device_type, grid_zone,
                         voltage, frequency, active_power, reactive_power, recorded_at )
                         VALUES ( ?, NOW(), ?, ?, ?, ?, ?, ?, NOW() )""";
        jdbcTemplate.update( sql, "device-0001", deviceType.name(), "gridZone", 230.01, 50.01, 12.40,  3.10);
    }
}