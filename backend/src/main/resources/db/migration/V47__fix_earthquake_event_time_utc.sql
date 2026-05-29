-- AFAD raw tarih alanı timezone içermiyor ve UTC bazlıdır.
-- Önceki parser bunu yanlışlıkla Istanbul (+3) olarak yorumladığından
-- tüm kayıtlar 3 saat eksik kaydedildi. Düzeltme: +3 saat ekle.
UPDATE earthquake_events
SET event_time = event_time + INTERVAL '3 hours';
