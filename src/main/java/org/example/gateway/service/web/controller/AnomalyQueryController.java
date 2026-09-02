package org.example.gateway.service.web.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.service.AnomalyQueryService;
import org.example.gateway.service.web.dto.Anomaly;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/anomalies")
@AllArgsConstructor
@Slf4j
public class AnomalyQueryController {

    private final AnomalyQueryService anomalyQueryService;

    @GetMapping("{meterId}/recent")
    public ResponseEntity<List<Anomaly>> getRecentReadings(
            @PathVariable String meterId,
            @RequestParam(defaultValue = "100") int limit) throws DatabaseException {

        log.debug("Received get recent anomalies for meterId = {}", meterId);
        var anomalies = anomalyQueryService.getRecentAnomalies(meterId, limit);
        return ResponseEntity.ok(anomalies);
    }

}
