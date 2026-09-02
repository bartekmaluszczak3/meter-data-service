package org.example.gateway.service.web.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.exception.InvalidInputException;
import org.example.gateway.service.service.AnomalyQueryService;
import org.example.gateway.service.web.dto.Anomaly;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/anomalies")
@AllArgsConstructor
@Slf4j
public class AnomalyQueryController {

    private final AnomalyQueryService anomalyQueryService;

    @GetMapping("{meterId}/recent")
    public ResponseEntity<List<Anomaly>> getRecentAnomalies(
            @PathVariable String meterId,
            @RequestParam(defaultValue = "100") int limit) throws DatabaseException {

        log.debug("Received get recent anomalies for meterId = {}", meterId);
        var anomalies = anomalyQueryService.getRecentAnomalies(meterId, limit);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("{meterId}/range")
    public ResponseEntity<List<Anomaly>> getReadingsInRange(
            @PathVariable String meterId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) throws InvalidInputException, DatabaseException {

        Instant effectiveTo = Objects.requireNonNullElse(to, Instant.now());
        var meterReadings =  anomalyQueryService.getAnomaliesInRange(meterId, from, effectiveTo);
        return ResponseEntity.ok(meterReadings);

    }

}
