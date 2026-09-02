package org.example.gateway.service.web.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.service.MeterQueryService;
import org.example.gateway.service.web.dto.MeterReading;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

}
