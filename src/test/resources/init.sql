CREATE TABLE IF NOT EXISTS meter_events (
    event_id VARCHAR(255) DEFAULT gen_random_uuid(),
    meter_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (event_id, occurred_at)
);

-- Create index for fast queries by meter_id and timestamp
CREATE INDEX IF NOT EXISTS idx_meter_events_meter_id
    ON meter_events(meter_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_meter_events_type
    ON meter_events(event_type);

-- Convert to TimescaleDB hypertable for optimized time-series storage
SELECT create_hypertable(
    'meter_events',
    'occurred_at',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '1 day'
);

-- Enable compression for older chunks (7+ days old)
ALTER TABLE meter_events SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'occurred_at DESC, event_id'
);

SELECT add_compression_policy(
    'meter_events',
    INTERVAL '7 days',
    if_not_exists => TRUE
);


CREATE TABLE IF NOT EXISTS meter_anomalies (
    anomaly_id UUID NOT NULL DEFAULT gen_random_uuid(),
    meter_id VARCHAR(255) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    event_occurred_at TIMESTAMPTZ NOT NULL,
    anomaly_type VARCHAR(100) NOT NULL,
    description TEXT,
    detected_value DECIMAL(10, 2),
    threshold DECIMAL(10, 2),
    severity VARCHAR(20),
    detected_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (anomaly_id, detected_at)
);

SELECT create_hypertable(
    'meter_anomalies',
    'detected_at',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '1 day'
);

CREATE INDEX IF NOT EXISTS idx_anomalies_meter_id
    ON meter_anomalies (meter_id, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_anomalies_severity
    ON meter_anomalies (severity, detected_at DESC);

ALTER TABLE meter_anomalies SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'detected_at DESC',
    timescaledb.compress_segmentby = 'meter_id, anomaly_type, severity'
);

SELECT add_compression_policy(
    'meter_anomalies',
    INTERVAL '7 days',
    if_not_exists => TRUE
);
