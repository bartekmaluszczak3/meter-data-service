package org.example.gateway.service.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class MeterEventService {

    private final MeterEventRepository eventRepository;

    public void save(MeterReadingRecordedEvent readingRecordedEvent) throws DatabaseException {
        log.debug("Saving reading event {}", readingRecordedEvent.getEventId());
        eventRepository.save(readingRecordedEvent);
        log.debug("Reading event saved {}", readingRecordedEvent.getEventId());

    }

    public void save(AnomalyDetectedEvent anomalyDetectedEvent) throws DatabaseException {
        log.debug("Saving anomaly {}", anomalyDetectedEvent.getAnomalyType().name());
        eventRepository.save(anomalyDetectedEvent);
        log.debug("Anomaly saved{}", anomalyDetectedEvent.getAnomalyType().name());

    }
}
