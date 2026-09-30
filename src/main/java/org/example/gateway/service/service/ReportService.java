package org.example.gateway.service.service;

import lombok.AllArgsConstructor;
import org.example.gateway.service.domain.repository.ReportRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.HourlyStatistics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;

    public List<HourlyStatistics> getReports(String meterId, Instant from, Instant to) throws DatabaseException {
        return reportRepository.getHourly(meterId,from, to);
    }
}
