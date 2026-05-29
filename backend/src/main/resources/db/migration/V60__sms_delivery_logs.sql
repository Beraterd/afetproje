-- V60: SMS delivery audit log.
-- phone_masked stores only the last 2 digits visible (e.g. 05** *** **12).

CREATE TABLE sms_delivery_logs (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        REFERENCES users(id) ON DELETE SET NULL,
    phone_masked        VARCHAR(20) NOT NULL,
    type                VARCHAR(30) NOT NULL,
    provider            VARCHAR(50) NOT NULL,
    provider_message_id VARCHAR(200),
    status              VARCHAR(20) NOT NULL,
    error_message       TEXT,
    earthquake_event_id UUID        REFERENCES earthquake_events(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at             TIMESTAMPTZ
);

CREATE INDEX idx_sms_delivery_logs_user_id    ON sms_delivery_logs(user_id);
CREATE INDEX idx_sms_delivery_logs_type       ON sms_delivery_logs(type);
CREATE INDEX idx_sms_delivery_logs_created_at ON sms_delivery_logs(created_at);
