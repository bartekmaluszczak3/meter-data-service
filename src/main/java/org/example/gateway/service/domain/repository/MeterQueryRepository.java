package org.example.gateway.service.domain.repository;

import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.MeterReading;

import java.time.Instant;
import java.util.List;

public interface MeterQueryRepository {

    List<MeterReading> getRecentReading(String meterId, int limit) throws DatabaseException;
    List<MeterReading> getReadingsInRange(String meterId, Instant from, Instant to) throws DatabaseException;

}
