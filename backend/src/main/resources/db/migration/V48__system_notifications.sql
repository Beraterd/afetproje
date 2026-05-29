-- System-wide notification center (belge onayı, deprem, kaynak talebi, vb.)
CREATE TABLE system_notifications (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID         REFERENCES users(id) ON DELETE CASCADE,
    target_role         VARCHAR(50),
    district_id         UUID         REFERENCES districts(id) ON DELETE CASCADE,
    neighborhood_id     UUID         REFERENCES neighborhoods(id) ON DELETE CASCADE,
    type                VARCHAR(50)  NOT NULL,
    title               VARCHAR(300) NOT NULL,
    message             TEXT,
    related_entity_type VARCHAR(100),
    related_entity_id   VARCHAR(100),
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    read_at             TIMESTAMPTZ
);

CREATE INDEX idx_sn_recipient    ON system_notifications(recipient_user_id) WHERE recipient_user_id IS NOT NULL;
CREATE INDEX idx_sn_target_role  ON system_notifications(target_role)       WHERE target_role IS NOT NULL;
CREATE INDEX idx_sn_district     ON system_notifications(district_id)       WHERE district_id IS NOT NULL;
CREATE INDEX idx_sn_neighborhood ON system_notifications(neighborhood_id)   WHERE neighborhood_id IS NOT NULL;
CREATE INDEX idx_sn_created_at   ON system_notifications(created_at DESC);
CREATE INDEX idx_sn_is_read      ON system_notifications(is_read);
