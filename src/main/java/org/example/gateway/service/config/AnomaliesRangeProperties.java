package org.example.gateway.service.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Data
@Configuration
@ConfigurationProperties(prefix = "service.anomalies-range")
public class AnomaliesRangeProperties {

    private SmartMeter smartMeter = new SmartMeter();
    private SolarPanel solarPanel = new SolarPanel();
    private WindTurbine windTurbine = new WindTurbine();
    private EVCharger evCharger = new EVCharger();
    private Battery battery = new Battery();

    @Data
    public static class SmartMeter {
        private Range voltage = new Range();
        private Range frequency = new Range();
        private Range reactivePower = new Range();;
    }

    @Data
    public static class SolarPanel {
        private Range voltage = new Range();
    }
    @Data
    public static class WindTurbine {
        private Range frequency = new Range();
    }

    @Data
    public static class EVCharger {
        private Range activePower = new Range();
    }
    @Data
    public static class Battery {
        private Range frequency = new Range();
    }

    @Data
    public static class Range {
        private Double min;
        private Double max;
    }
}