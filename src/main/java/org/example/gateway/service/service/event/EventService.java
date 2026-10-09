package org.example.gateway.service.service.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class EventService {

    private final KafkaTemplate<String, AnomalyDetectedEvent> kafkaTemplate;
    private final static String TOPIC = "anomaly-detected";

    public void sendEvent(AnomalyDetectedEvent event) {
        log.info("Sending anomaly detected event");
        kafkaTemplate.send(TOPIC, event.getMeterId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Cannot send event for meterId={}: {}",
                                event.getMeterId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Event sent meterId={} partition={} offset={}", event.getMeterId(), result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
