package org.example.gateway.service.web;

import org.example.gateway.service.utils.IntegrationBaseTest;
import org.example.gateway.service.web.dto.*;
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

    @Test
    void shouldFindReadingUsingTimestamp() {
        // given
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T10:00:00Z");
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name(), Instant.parse("2026-08-01T00:00:00Z"));
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name(),  Instant.parse("2026-09-01T01:00:00Z"));
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name(),  Instant.parse("2026-09-02T00:00:00Z"));

        String url = "/api/v1/anomalies/device-0001/range"
                + "?from=" + from.toString()
                + "&to=" + to.toString();
        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<Anomaly>>() {
                });

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        Assertions.assertEquals(2, body.size());
        body.forEach(e -> {
            Assertions.assertTrue(from.isBefore(e.detectedAt()));
            Assertions.assertTrue(to.isAfter(e.detectedAt()));
        });
    }

    @Test
    void shouldUseNowIfToIsNotSet() {
        // given
        Instant from = Instant.parse("2025-09-01T00:00:00Z");
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name(), Instant.parse("2025-10-01T00:00:00Z"));
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name(),  Instant.parse("2025-11-01T01:00:00Z"));
        createAnomaly(AnomalyType.FREQUENCY_DEVIATION.name(),  Instant.parse("2025-09-02T00:00:00Z"));

        String url = "/api/v1/anomalies/device-0001/range"
                + "?from=" + from.toString();
        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<List<Anomaly>>() {
                });

        // then
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        var body = response.getBody();
        System.out.println(body);
        Assertions.assertEquals(3, body.size());
    }

    @Test
    void shouldThrowExceptionWhenFromIsAfterTo() {
        // given
        Instant to = Instant.parse("2026-09-01T00:00:00Z");
        Instant from = Instant.parse("2026-09-02T10:00:00Z");

        String url = "/api/v1/anomalies/device-0001/range"
                + "?from=" + from.toString()
                + "&to=" + to.toString();
        // when
        var response = testRestTemplate.exchange(url, HttpMethod.GET,  new HttpEntity<>(new HttpHeaders()), String.class);

        // then
        Assertions.assertTrue(response.getStatusCode().is5xxServerError());
    }

    private void createAnomaly(String anomalyType) {
        String sql = """
                INSERT INTO meter_anomalies (anomaly_id,detected_at,meter_id, anomaly_type,description,detected_value,threshold,severity)
                VALUES (gen_random_uuid(), NOW(), 'device-0001',?,'Detected anomaly', 123.45,100.00,'INFO')""";
        jdbcTemplate.update(sql, anomalyType);
    }

    private void createAnomaly(String anomalyType, Instant readingTimestamp) {
        String sql = """
                INSERT INTO meter_anomalies (anomaly_id,detected_at,meter_id, anomaly_type,description,detected_value,threshold,severity)
                VALUES (gen_random_uuid(), ?, 'device-0001',?,'Detected anomaly', 123.45,100.00,'INFO')""";
        jdbcTemplate.update(sql, Timestamp.from(readingTimestamp), anomalyType);
    }

}
