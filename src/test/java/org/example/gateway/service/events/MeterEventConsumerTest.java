package org.example.gateway.service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.domain.value.DeviceType;
import org.example.gateway.service.Application;
import org.example.gateway.service.aggregate.MeterAggregate;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.service.MeterEventService;
import org.junit.jupiter.api.Assertions;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.gateway.domain.value.DeviceType.SMART_METER;
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
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    static DockerImageName TIMESCALE_IMAGE =
            DockerImageName.parse("timescale/timescaledb:2.17.2-pg15")
                    .asCompatibleSubstituteFor("postgres");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TIMESCALE_IMAGE)
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
    private MeterEventService meterEventService;

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
                    MeterAggregate meterState = meterEventService.getMeterState(meterId);
                    assertThat(meterState.getMeterId()).isEqualTo(meterId);
                });
    }

    @SneakyThrows
    @Test
    void shouldPublishToDifferentTopic(){

        // when
        kafkaTemplate.send("telemetry.solar", "solar-001", buildPayload("solar-001",  DeviceType.SOLAR_PANEL));
        kafkaTemplate.send("telemetry.wind", "wind-001", buildPayload("wind-001",  DeviceType.WIND_TURBINE));
        kafkaTemplate.send("telemetry.ev", "ev-001", buildPayload("ev-001",  DeviceType.EV_CHARGER));
        kafkaTemplate.send("telemetry.battery", "battery-001", buildPayload("battery-001",  DeviceType.BATTERY_STORAGE));

        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<String> metersId = List.of("solar-001","wind-001", "ev-001", "battery-001" );
                    for(String meter: metersId) {
                        MeterAggregate meterState = meterEventService.getMeterState(meter);
                        assertThat(meterState.getMeterId()).isEqualTo(meter);
                    }

                });
    }

    @Test
    void shouldDetectAnomalyInMeter() throws InterruptedException {
        String meterId = "meter-001";
        String topic = "telemetry.meters";
        Readings readings = new Readings(
                150.1, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null);
        var payload = new TelemetryPayload(meterId, SMART_METER, Instant.now(), readings);

        kafkaTemplate.send(topic, meterId, payload);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    MeterAggregate meterState = meterEventService.getMeterState(meterId);
                    var anomalies = meterState.getAnomalies();
                    Assertions.assertEquals(3, anomalies.size());
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
