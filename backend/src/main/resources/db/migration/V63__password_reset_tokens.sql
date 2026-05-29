CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ip_address   VARCHAR(100),
    user_agent   VARCHAR(500)
);

CREATE INDEX  IF NOT EXISTS idx_prt_user_id    ON password_reset_tokens(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_prt_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX  IF NOT EXISTS idx_prt_expires_at ON password_reset_tokens(expires_at);
