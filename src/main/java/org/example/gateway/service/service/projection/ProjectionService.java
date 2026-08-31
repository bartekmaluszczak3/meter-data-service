package org.example.gateway.service.service.projection;

import lombok.AllArgsConstructor;
import org.example.gateway.service.domain.event.MeterReadingRecordedEvent;
import org.example.gateway.service.domain.repository.projection.ProjectionRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProjectionService {

    private final ProjectionRepository projectionRepository;
    public void save(MeterReadingRecordedEvent readingEvent) throws DatabaseException {
        projectionRepository.save(readingEvent);
    }
}
