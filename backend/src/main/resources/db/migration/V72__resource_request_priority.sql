-- =====================================================================
-- V72: Kaynak taleplerine öncelik (priority) ve birim (unit) alanları
-- =====================================================================

ALTER TABLE resource_requests
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE resource_requests
    ADD COLUMN unit VARCHAR(30);
