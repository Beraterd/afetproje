-- V61: Email notification delivery log table.

CREATE TABLE email_notification_logs (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id    UUID         REFERENCES users(id) ON DELETE SET NULL,
    recipient_email      VARCHAR(255) NOT NULL,
    type                 VARCHAR(60)  NOT NULL,
    subject              VARCHAR(500),
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message        TEXT,
    related_entity_type  VARCHAR(100),
    related_entity_id    VARCHAR(100),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at              TIMESTAMPTZ
);

CREATE INDEX idx_email_notif_logs_user_id   ON email_notification_logs(recipient_user_id);
CREATE INDEX idx_email_notif_logs_type      ON email_notification_logs(type);
CREATE INDEX idx_email_notif_logs_status    ON email_notification_logs(status);
CREATE INDEX idx_email_notif_logs_created   ON email_notification_logs(created_at);
