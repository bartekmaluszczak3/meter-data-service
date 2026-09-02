package org.example.gateway.service.web;

import org.example.gateway.service.utils.IntegrationBaseTest;
import org.example.gateway.service.web.dto.Anomaly;
import org.example.gateway.service.web.dto.AnomalyType;
import org.example.gateway.service.web.dto.DeviceType;
import org.example.gateway.service.web.dto.Severity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.List;

public class GetAnomalyTest extends IntegrationBaseTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void beforeEach() {
        clear();
    }

    @Test
    void shouldReturnAnomaly() {
        // given
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name());
        String url = "/api/v1/anomalies/device-0001/recent";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<Anomaly>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(1, body.size());
        var anomaly = body.get(0);
        Assertions.assertEquals("device-0001", anomaly.meterId());
        Assertions.assertEquals(AnomalyType.FREQUENCY_DEVIATION, anomaly.anomalyType());
        Assertions.assertEquals("Detected anomaly", anomaly.description());
        Assertions.assertEquals(123.45, anomaly.detectedValue());
        Assertions.assertEquals(100.0, anomaly.threshold());
        Assertions.assertEquals(Severity.INFO, anomaly.severity());
    }

    @Test
    void shouldLimitAnomalies() {
        // given
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name());
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name());
        createAnomaly(AnomalyType.FREQUENCY_INSTABILITY.name());
        createAnomaly(AnomalyType.HIGH_REACTIVE_POWER.name());
        createAnomaly(AnomalyType.OVERCURRENT.name());
        createAnomaly(AnomalyType.OVERVOLTAGE.name());

        String url = "/api/v1/anomalies/device-0001/recent?limit=3";

        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<Anomaly>>() {});

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(3, body.size());
    }

    private void createAnomaly(String anomalyType) {
        String sql = """
                INSERT INTO meter_anomalies (anomaly_id,detected_at,meter_id, anomaly_type,description,detected_value,threshold,severity)
                VALUES (gen_random_uuid(), NOW(), 'device-0001',?,'Detected anomaly', 123.45,100.00,'INFO')""";
        jdbcTemplate.update(sql, anomalyType);
    }

}
