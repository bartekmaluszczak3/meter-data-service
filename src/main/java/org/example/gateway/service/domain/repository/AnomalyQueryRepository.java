package org.example.gateway.service.domain.repository;

import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.Anomaly;

import java.util.List;

public interface AnomalyQueryRepository {

    List<Anomaly> getRecentAnomaly(String meterId, int limit) throws DatabaseException;

}
