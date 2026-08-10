package org.example.gateway.service.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.aggregate.MeterAggregate;
import org.example.gateway.service.domain.event.AnomalyDetectedEvent;
import org.example.gateway.service.domain.event.DomainEvent;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class MeterEventService {

    private final MeterEventRepository eventRepository;

    @Transactional(readOnly = true)
    public MeterAggregate getMeterState(String meterId) throws DatabaseException {
        List<DomainEvent> events = eventRepository.getEventStream(meterId);

        MeterAggregate meter = new MeterAggregate();
        meter.rebuildFromEvents(events);

        return meter;
    }

    @Transactional
    public void save(MeterReadingRecordedEvent readingRecordedEvent) throws DatabaseException {
        eventRepository.save(readingRecordedEvent);
    }

    @Transactional
    public void save(AnomalyDetectedEvent anomalyDetectedEvent) throws DatabaseException {
        eventRepository.save(anomalyDetectedEvent);
    }
}
