-- =====================================================================
-- V15: Kaynak ve Ekipman İhtiyaç Yönetimi tablosu
-- =====================================================================

CREATE TABLE resource_requests (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id          UUID         NOT NULL REFERENCES districts(id),
    neighborhood_id      UUID                  REFERENCES neighborhoods(id),
    request_scope        VARCHAR(20)  NOT NULL DEFAULT 'NEIGHBORHOOD',
    resource_type        VARCHAR(50)  NOT NULL,
    quantity             INTEGER,
    urgency              SMALLINT     NOT NULL DEFAULT 3,
    status               VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    title                VARCHAR(255) NOT NULL,
    description          TEXT,
    created_by_user_id   UUID         NOT NULL REFERENCES users(id),
    assigned_to_user_id  UUID                  REFERENCES users(id),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at          TIMESTAMPTZ
);

CREATE INDEX idx_rr_district       ON resource_requests(district_id);
CREATE INDEX idx_rr_neighborhood   ON resource_requests(neighborhood_id);
CREATE INDEX idx_rr_status         ON resource_requests(status);
CREATE INDEX idx_rr_resource_type  ON resource_requests(resource_type);
CREATE INDEX idx_rr_urgency        ON resource_requests(urgency);
