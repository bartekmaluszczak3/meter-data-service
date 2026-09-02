package org.example.gateway.service.domain.repository;

import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.MeterReading;

import java.util.List;

public interface MeterQueryRepository {

    List<MeterReading> getRecentReading(String meterId, int limit) throws DatabaseException;
}
