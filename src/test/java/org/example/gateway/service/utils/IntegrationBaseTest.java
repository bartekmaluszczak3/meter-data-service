package org.example.gateway.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.Application;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.service.MeterEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public abstract class IntegrationBaseTest {

    private static final DockerImageName TIMESCALE_IMAGE =
            DockerImageName.parse("timescale/timescaledb:2.17.2-pg15")
                    .asCompatibleSubstituteFor("postgres");

    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(TIMESCALE_IMAGE)
                    .withDatabaseName("test_gridflow")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("init.sql");

    protected static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
            );

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }

    @Autowired
    protected KafkaTemplate<String, TelemetryPayload> kafkaTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MeterEventService meterEventService;

    @Autowired
    protected MeterEventRepository eventRepository;


    protected Integer count() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM meter_events",
                Integer.class
        );
    }

    protected List<Map<String, Object>> getAllRows() {
        return jdbcTemplate.queryForList(
                "SELECT * FROM meter_events"
        );
    }

    protected void clear() {
        jdbcTemplate.update("DELETE FROM meter_events");
    }

    protected TelemetryPayload buildPayload(
            String deviceId,
            DeviceType type
    ) {
        Readings readings = new Readings(
                230.1, 50.0, 3.45, 0.5,
                null, null, null, null,
                null, null, null, null, null
        );

        return new TelemetryPayload(
                deviceId,
                type,
                Instant.now(),
                readings
        );
    }
}
