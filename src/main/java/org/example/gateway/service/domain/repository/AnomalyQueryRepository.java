package org.example.gateway.service.domain.repository;

import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.Anomaly;

import java.time.Instant;
import java.util.List;

public interface AnomalyQueryRepository {

    List<Anomaly> getRecentAnomaly(String meterId, int limit) throws DatabaseException;

    List<Anomaly> getAnomaliesInRange(String meterId, Instant from, Instant to) throws DatabaseException;
}
