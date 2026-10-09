package org.example.gateway.service.event;

import lombok.SneakyThrows;
import org.example.gateway.domain.Readings;
import org.example.gateway.domain.TelemetryPayload;
import org.example.gateway.service.Application;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.example.gateway.service.service.anomaly.AnomalyDetectionService;
import org.example.gateway.service.utils.KafkaTestConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.example.gateway.domain.value.DeviceType.SMART_METER;

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class AnomalyDetectedEventTest {

    private KafkaTestConsumer kafkaConsumer;

    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
            );

    static {
        kafka.start();
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @SneakyThrows
    @BeforeEach
    void beforeEach(){
        kafkaConsumer = new KafkaTestConsumer(kafka.getBootstrapServers());

    }

    @SneakyThrows
    @Test
    void shouldSendThreeEvents() {
        // given
        Readings readings = new Readings(
                150.1, 5.0, 3.45, 20.0,
                null, null, null, null,
                null, null, null, null, null
        );
        var payload = new TelemetryPayload(UUID.randomUUID().toString(), SMART_METER, Instant.now(), readings);

        // when
        anomalyDetectionService.detectAnomalies(payload);
        List<AnomalyDetectedEvent> events = kafkaConsumer.consumeEvents("anomaly-detected", Duration.ofSeconds(2), AnomalyDetectedEvent.class);

        // then
        Assertions.assertEquals(3, events.size());
    }

}
