package org.example.gateway.service.web.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.exception.InvalidInputException;
import org.example.gateway.service.service.MeterQueryService;
import org.example.gateway.service.web.dto.MeterReading;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/meters")
@AllArgsConstructor
@Slf4j
public class MeterQueryController {

    private final MeterQueryService meterQueryService;

    @GetMapping("{meterId}/recent")
    public ResponseEntity<List<MeterReading>> getRecentReadings(
            @PathVariable String meterId,
            @RequestParam(defaultValue = "100") int limit) throws DatabaseException {

        log.debug("Received get recent readings for meterId = {}", meterId);
        var meterReadings = meterQueryService.getRecentReadings(meterId, limit);
        return ResponseEntity.ok(meterReadings);
    }

    @GetMapping("{meterId}/range")
    public ResponseEntity<List<MeterReading>> getReadingsInRange(
            @PathVariable String meterId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) throws InvalidInputException, DatabaseException {

        Instant effectiveTo = Objects.requireNonNullElse(to, Instant.now());
        var meterReadings =  meterQueryService.getReadingsInRange(meterId, from, effectiveTo);
        return ResponseEntity.ok(meterReadings);

    }

}
