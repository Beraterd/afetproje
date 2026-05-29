-- V59: SMS two-step login challenge table.
-- Stores BCrypt-hashed OTP codes; plain-text code is never persisted.

CREATE TABLE sms_login_codes (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone_masked   VARCHAR(20) NOT NULL,
    code_hash      VARCHAR(255) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    used_at        TIMESTAMPTZ,
    attempt_count  INTEGER     NOT NULL DEFAULT 0,
    ip_address     VARCHAR(45),
    user_agent     VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sms_login_codes_user_id    ON sms_login_codes(user_id);
CREATE INDEX idx_sms_login_codes_expires_at ON sms_login_codes(expires_at);
