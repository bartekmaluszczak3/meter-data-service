CREATE TABLE meter_events (
    event_id VARCHAR(255) PRIMARY KEY,
    meter_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    version INTEGER NOT NULL
);