
CREATE TABLE IF NOT EXISTS meter_events (
    event_id VARCHAR(36) NOT NULL,
    meter_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convert to TimescaleDB hypertable (1-day chunks)
SELECT create_hypertable('meter_events', 'timestamp', if_not_exists => TRUE);

-- PRIMARY KEY MUST include timestamp (partitioning column)
ALTER TABLE meter_events ADD CONSTRAINT pk_meter_events
    PRIMARY KEY (event_id, timestamp);

-- Set compression (compress after 7 days)
ALTER TABLE meter_events SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'meter_id',
    timescaledb.compress_orderby = 'timestamp DESC'
);

SELECT add_compression_policy('meter_events', INTERVAL '7 days', if_not_exists => TRUE);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_meter_events_meter_id_timestamp
ON meter_events(meter_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_meter_events_event_type
ON meter_events(event_type);

COMMENT ON TABLE meter_events IS 'Event Store - Append-only, immutable event log';

-- ============================================================================
-- MATERIALIZED VIEW 1: READINGS (Projection)
-- ============================================================================

CREATE TABLE IF NOT EXISTS meter_readings_materialized (
    meter_id VARCHAR(100) NOT NULL,
    reading_timestamp TIMESTAMPTZ NOT NULL,
    device_type VARCHAR(50),
    grid_zone VARCHAR(100),
    voltage DECIMAL(10, 2),
    frequency DECIMAL(10, 2),
    active_power DECIMAL(10, 2),
    reactive_power DECIMAL(10, 2),
    recorded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convert to TimescaleDB hypertable (1-day chunks)
SELECT create_hypertable('meter_readings_materialized', 'reading_timestamp', if_not_exists => TRUE);

-- PRIMARY KEY MUST include reading_timestamp (partitioning column)
ALTER TABLE meter_readings_materialized ADD CONSTRAINT pk_meter_readings
    PRIMARY KEY (meter_id, reading_timestamp);

-- Compression (compress after 30 days)
ALTER TABLE meter_readings_materialized SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'meter_id,device_type',
    timescaledb.compress_orderby = 'reading_timestamp DESC'
);

SELECT add_compression_policy('meter_readings_materialized', INTERVAL '30 days', if_not_exists => TRUE);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_meter_readings_device_type
ON meter_readings_materialized(device_type, reading_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_meter_readings_grid_zone
ON meter_readings_materialized(grid_zone, reading_timestamp DESC);

COMMENT ON TABLE meter_readings_materialized IS 'CQRS Read Model - Denormalized readings';

-- ============================================================================
-- MATERIALIZED VIEW 2: ANOMALIES (Projection)
-- ============================================================================

CREATE TABLE IF NOT EXISTS meter_anomalies (
    anomaly_id VARCHAR(36) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    meter_id VARCHAR(100) NOT NULL,
    anomaly_type VARCHAR(100) NOT NULL,
    description TEXT,
    detected_value DECIMAL(10, 2),
    threshold DECIMAL(10, 2),
    severity VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convert to TimescaleDB hypertable (1-day chunks)
SELECT create_hypertable('meter_anomalies', 'detected_at', if_not_exists => TRUE);

-- PRIMARY KEY MUST include detected_at (partitioning column)
ALTER TABLE meter_anomalies ADD CONSTRAINT pk_meter_anomalies
    PRIMARY KEY (anomaly_id, detected_at);

-- Compression (compress after 30 days)
ALTER TABLE meter_anomalies SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'meter_id,anomaly_type',
    timescaledb.compress_orderby = 'detected_at DESC'
);

SELECT add_compression_policy('meter_anomalies', INTERVAL '30 days', if_not_exists => TRUE);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_meter_anomalies_meter_id_detected
ON meter_anomalies(meter_id, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_meter_anomalies_severity
ON meter_anomalies(severity, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_meter_anomalies_type
ON meter_anomalies(anomaly_type, detected_at DESC);

COMMENT ON TABLE meter_anomalies IS 'CQRS Read Model - Dedicated anomalies table';
CREATE MATERIALIZED VIEW meter_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', reading_timestamp) AS hour,
    meter_id,
    device_type,
    grid_zone,
    COUNT(*) AS reading_count,
    AVG(voltage) AS avg_voltage,
    MIN(voltage) AS min_voltage,
    MAX(voltage) AS max_voltage,
    AVG(frequency) AS avg_frequency,
    AVG(active_power) AS avg_active_power,
    AVG(reactive_power) AS avg_reactive_power
FROM meter_readings_materialized
GROUP BY
    hour,
    meter_id,
    device_type,
    grid_zone;

SELECT add_continuous_aggregate_policy(
    'meter_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);

CREATE INDEX IF NOT EXISTS idx_meter_readings_hourly_hour_meter
    ON meter_readings_hourly(hour DESC, meter_id);

-- ============================================================================
-- HELPER VIEWS
-- ============================================================================

-- View: Meters with recent anomalies
CREATE OR REPLACE VIEW v_meters_with_anomalies AS
SELECT
    meter_id,
    COUNT(*) as anomaly_count,
    MAX(detected_at) as last_anomaly,
    MAX(severity) as max_severity
FROM meter_anomalies
WHERE detected_at > NOW() - INTERVAL '24 hours'
GROUP BY meter_id;

-- View: Event Store stats
CREATE OR REPLACE VIEW v_event_store_stats AS
SELECT
    event_type,
    COUNT(*) as event_count,
    MIN(timestamp) as first_event,
    MAX(timestamp) as last_event
FROM meter_events
GROUP BY event_type;