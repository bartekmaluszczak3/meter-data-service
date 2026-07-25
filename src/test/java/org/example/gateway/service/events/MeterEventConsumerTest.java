package org.example.gateway.service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.Application;
import org.example.gateway.service.aggregate.MeterAggregate;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.service.MeterEventConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Testcontainers
public class MeterEventConsumerTest {
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_gridflow")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @Autowired
    private KafkaTemplate<String, TelemetryPayload> kafkaTemplate;

    @Autowired
    private MeterEventConsumer meterEventConsumer;

    @Autowired
    private MeterEventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    @Test
    void shouldProperlyReceiveEventAndSaveToDatabase(){
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        TelemetryPayload payload = buildPayload("meter-001", DeviceType.SMART_METER);

        kafkaTemplate.send(topic, meterId, payload);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    MeterAggregate meterState = meterEventConsumer.getMeterState(meterId);
                    assertThat(meterState.getMeterId()).isEqualTo(meterId);
                });
    }

    private TelemetryPayload buildPayload(String deviceId, DeviceType type) {
        Readings readings = new Readings(
                230.1, 50.0, 3.45, 0.5,
                null, null, null, null,
                null, null, null, null, null
        );
        return new TelemetryPayload(deviceId, type, Instant.now(), readings);
    }
}
