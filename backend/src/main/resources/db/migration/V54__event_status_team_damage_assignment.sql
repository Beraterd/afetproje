-- V54: Add new enum values only
--
-- Schema analysis:
--   • team_name           → PostgreSQL ENUM (V1)  → ALTER TYPE needed
--   • event_status        → PostgreSQL ENUM (V1)  → ALTER TYPE needed
--   • verification_status → VARCHAR(30) column     → NO ALTER TYPE needed;
--                           JPA @Enumerated(STRING) writes the string directly
--
-- NOTE: ALTER TYPE ADD VALUE is non-transactional in PostgreSQL.
--       DML that uses the new enum values MUST run in a separate transaction
--       (see V55). Flyway wraps each migration in its own transaction, so
--       splitting here mirrors the V7 → V8 pattern already in this project.

-- 1. New team type: Hasar Tespit Ekibi
ALTER TYPE team_name ADD VALUE IF NOT EXISTS 'HASAR_TESPIT_EKIBI';

-- 2. New event statuses: devam ediyor + tamamlandı
ALTER TYPE event_status ADD VALUE IF NOT EXISTS 'IN_PROGRESS';
ALTER TYPE event_status ADD VALUE IF NOT EXISTS 'COMPLETED';

-- verification_status is VARCHAR(30) — no ALTER TYPE needed.
-- The Java enum value 'ASSIGNED' will be stored as the literal string
-- by Hibernate @Enumerated(EnumType.STRING). No DDL change required.
