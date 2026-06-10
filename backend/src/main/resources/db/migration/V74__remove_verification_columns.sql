-- V74: Remove email-verification and SMS-OTP-login infrastructure.
-- Email verification is no longer required; users are active upon creation.
-- SMS OTP 2FA login is removed; standard email+password login remains.
--
-- email_verified was effectively always FALSE (workflow was never activated).
-- pending_email was effectively always NULL.
-- Dropping email_verification_tokens and sms_login_codes tables entirely.
-- sms_delivery_logs is KEPT — it records earthquake-alert SMS delivery history.

ALTER TABLE users DROP COLUMN IF EXISTS email_verified;
ALTER TABLE users DROP COLUMN IF EXISTS pending_email;

DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS sms_login_codes;
