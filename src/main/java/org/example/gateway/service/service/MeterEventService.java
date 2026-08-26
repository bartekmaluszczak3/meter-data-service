package org.example.gateway.service.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class MeterEventService {

    private final MeterEventRepository eventRepository;

    @Transactional
    public void save(MeterReadingRecordedEvent readingRecordedEvent) throws DatabaseException {
        eventRepository.save(readingRecordedEvent);
    }

    @Transactional
    public void save(AnomalyDetectedEvent anomalyDetectedEvent, String eventId, Instant eventOccurredAt) throws DatabaseException {
        eventRepository.save(anomalyDetectedEvent, eventId, eventOccurredAt);
    }
}
