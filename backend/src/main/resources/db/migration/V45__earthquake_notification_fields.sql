ALTER TABLE earthquake_events
    ADD COLUMN notification_sent     BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN notification_sent_at  TIMESTAMPTZ;

CREATE INDEX idx_earthquake_events_notification_sent ON earthquake_events(notification_sent)
    WHERE notification_sent = FALSE;
