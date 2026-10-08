package org.example.gateway.service.web.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.service.report.ReportService;
import org.example.gateway.service.web.dto.HourlyStatistics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@AllArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/meters/{meterId}/daily")
    public ResponseEntity<List<HourlyStatistics>> getHourlyStatistics(@PathVariable String meterId) throws DatabaseException {
        Instant now = Instant.now();
        Instant from = Instant.now().minus(24, ChronoUnit.HOURS);
        var hourlyStats = reportService.getReports(meterId, from, now);
        return ResponseEntity.ok(hourlyStats);
    }

    @GetMapping("/meters/{meterId}/weekly")
    public ResponseEntity<List<HourlyStatistics>> getWeeklyStatistics(@PathVariable String meterId) throws DatabaseException {
        Instant now = Instant.now();
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        var hourlyStats = reportService.getReports(meterId, from, now);
        return ResponseEntity.ok(hourlyStats);
    }
}
