package org.example.gateway.service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.repository.MeterQueryRepository;
import org.example.gateway.service.exception.DatabaseException;
import org.example.gateway.service.web.dto.MeterReading;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class MeterQueryService {
    private final MeterQueryRepository meterQueryRepository;

    public List<MeterReading> getRecentReadings(String meterId, int limit) throws DatabaseException {
        return meterQueryRepository.getRecentReading(meterId, limit);
    }
}
