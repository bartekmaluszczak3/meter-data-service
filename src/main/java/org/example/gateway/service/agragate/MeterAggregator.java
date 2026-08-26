package org.example.gateway.service.agragate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.service.domain.event.AnomalyType;
import org.example.gateway.service.domain.event.Severity;
import org.example.gateway.service.domain.internal.Anomaly;
import org.example.gateway.service.domain.internal.MeterEvent;
import org.example.gateway.service.domain.repository.MeterEventRepository;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class MeterAggregator {

    private final MeterEventRepository meterEventRepository;
    private final ObjectMapper objectMapper;


    public List<MeterEvent> getMeterEvents() throws JsonProcessingException {
        List<Map<String, Object>> eventsFromDataBase = meterEventRepository.getMeterEvents();
        List<MeterEvent> meterEvents = new ArrayList<>();
        for(Map<String, Object> databaseEntry: eventsFromDataBase){
            PGobject eventData = (PGobject) databaseEntry.get("event_data");
            Map<String, Object> data = objectMapper.readValue(eventData.getValue(),
                    new TypeReference<Map<String, Object>>() {});
            MeterEvent meterEvent = MeterEvent.builder()
                    .eventId(String.valueOf(databaseEntry.get("event_id")))
                    .meterId(String.valueOf(databaseEntry.get("meter_id")))
                    .eventType(String.valueOf(databaseEntry.get("event_type")))
                    .occurredAt(String.valueOf(databaseEntry.get("occurred_at")))
                    .version(Integer.parseInt(String.valueOf(databaseEntry.get("version"))))
                    .deviceType(String.valueOf(data.get("deviceType")))
                    .gridZone(String.valueOf(data.get("gridZone")))
                    .voltage(Double.parseDouble(String.valueOf(data.get("voltage"))))
                    .frequency(Double.parseDouble(String.valueOf(data.get("frequency"))))
                    .activePower(Double.parseDouble(String.valueOf(data.get("activePower"))))
                    .reactivePower(Double.parseDouble(String.valueOf(data.get("reactivePower"))))
                    .readingTimestamp(String.valueOf(data.get("readingTimestamp")))
                    .recordedAt(String.valueOf(data.get("recordedAt")))
                    .build();
            meterEvents.add(meterEvent);
        };
        return meterEvents;
    }

    public List<Anomaly> getAnomalies()  {
        List<Map<String, Object>> anomaliesFromDataBase = meterEventRepository.getAnomalies();
        List<Anomaly> anomalies = new ArrayList<>();
        for(Map<String, Object> databaseEntry: anomaliesFromDataBase){
            Anomaly anomaly = Anomaly.builder()
                    .anomalyId(String.valueOf(databaseEntry.get("anomaly_id")))
                    .meterId(String.valueOf(databaseEntry.get("meter_id")))
                    .eventId(String.valueOf(databaseEntry.get("event_id")))
                    .eventOccurredAt(String.valueOf(databaseEntry.get("event_occurred_at")))
                    .anomalyType(AnomalyType.valueOf(String.valueOf(databaseEntry.get("anomaly_type"))))
                    .description(String.valueOf(databaseEntry.get("description")))
                    .detectedValue(Double.parseDouble(String.valueOf(databaseEntry.get("detected_value"))))
                    .threshold(Double.parseDouble(String.valueOf(databaseEntry.get("threshold"))))
                    .severity(Severity.valueOf(String.valueOf(databaseEntry.get("severity"))))
                    .detectedAt(String.valueOf(databaseEntry.get("detected_at")))
                    .createdAt(String.valueOf(databaseEntry.get("created_at")))
                    .build();
            anomalies.add(anomaly);

        };
        return anomalies;
    }

}
