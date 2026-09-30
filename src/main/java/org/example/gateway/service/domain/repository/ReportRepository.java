package org.example.gateway.service.domain.repository;

import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.HourlyStatistics;

import java.time.Instant;
import java.util.List;

public interface ReportRepository {

    List<HourlyStatistics> getHourly(String meterId, Instant from, Instant to) throws DatabaseException;
}
