-- =====================================================================
-- V69: Acil durum mesaj loglarına WhatsApp kanalı desteği
--  - recipient_email artık nullable (WhatsApp logunda e-posta olmayabilir)
--  - recipient_phone kolonu eklendi (WhatsApp/maskelenmiş numara)
-- =====================================================================

ALTER TABLE emergency_status_message_logs
    ALTER COLUMN recipient_email DROP NOT NULL;

ALTER TABLE emergency_status_message_logs
    ADD COLUMN recipient_phone VARCHAR(32);
