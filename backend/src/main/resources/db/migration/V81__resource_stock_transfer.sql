-- Kaynak transfer önerisi + optimistic locking desteği için resource_stocks'a version kolonu.
-- DEFAULT 0 mevcut kayıtları bozmadan güvenli şekilde eklenir.
ALTER TABLE resource_stocks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
