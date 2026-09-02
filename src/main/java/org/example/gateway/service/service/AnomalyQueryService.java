package org.example.gateway.service.service;

import lombok.RequiredArgsConstructor;
import org.example.gateway.service.domain.repository.AnomalyQueryRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.exception.InvalidInputException;
import org.example.gateway.service.web.dto.Anomaly;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalyQueryService {

    private final AnomalyQueryRepository anomalyQueryRepository;
    public  List<Anomaly> getRecentAnomalies(String meterId, int limit) throws DatabaseException {
        return anomalyQueryRepository.getRecentAnomaly(meterId, limit);
    }

    public List<Anomaly> getAnomaliesInRange(String meterId, Instant from, Instant to) throws DatabaseException, InvalidInputException {
        if(from.isAfter(to)){
            throw new InvalidInputException("To cannot be before from");
        }
        return anomalyQueryRepository.getAnomaliesInRange(meterId, from, to);

    }
}
