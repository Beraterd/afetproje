CREATE TABLE earthquake_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    depth DOUBLE PRECISION,
    magnitude DOUBLE PRECISION NOT NULL,
    location TEXT,
    province VARCHAR(255),
    district VARCHAR(255),
    source VARCHAR(50) NOT NULL DEFAULT 'AFAD',
    risk_level VARCHAR(20) NOT NULL,
    raw_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_earthquake_events_external_id ON earthquake_events(external_id);
CREATE INDEX idx_earthquake_events_event_time ON earthquake_events(event_time DESC);
CREATE INDEX idx_earthquake_events_magnitude ON earthquake_events(magnitude);
CREATE INDEX idx_earthquake_events_risk_level ON earthquake_events(risk_level);
