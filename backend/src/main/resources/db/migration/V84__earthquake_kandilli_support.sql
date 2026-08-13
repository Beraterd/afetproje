-- Kandilli Rasathanesi entegrasyonu için earthquake_events genişletmesi.
-- source artık enum (AFAD/KANDILLI) olarak Java tarafında yönetiliyor ama DB kolonu
-- zaten VARCHAR idi (Hibernate STRING enum mapping aynı kolon tipini kullanır) — şema
-- değişikliği gerekmiyor, yalnızca CHECK constraint ile değer kümesi sınırlanıyor.

ALTER TABLE earthquake_events ADD COLUMN magnitude_type VARCHAR(10);
ALTER TABLE earthquake_events ADD COLUMN province_normalized VARCHAR(100);
ALTER TABLE earthquake_events ADD COLUMN district_normalized VARCHAR(100);

-- Mevcut (yalnızca AFAD) kayıtlar için geriye dönük normalize backfill — Türkçe karakterleri
-- ASCII eşdeğerine çevirip büyük harfe çeviren aynı kural Java LocationNormalizer'da da uygulanır.
UPDATE earthquake_events SET province_normalized = UPPER(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        province,
        'İ','I'),'ı','I'),'I','I'),'i','I'),
        'Ş','S'),'ş','S'),
        'Ç','C'),'ç','C'),
        'Ğ','G'),'ğ','G'),
        'Ö','O'),'ö','O'),
        'Ü','U'),'ü','U')
) WHERE province IS NOT NULL;

UPDATE earthquake_events SET district_normalized = UPPER(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        district,
        'İ','I'),'ı','I'),'I','I'),'i','I'),
        'Ş','S'),'ş','S'),
        'Ç','C'),'ç','C'),
        'Ğ','G'),'ğ','G'),
        'Ö','O'),'ö','O'),
        'Ü','U'),'ü','U')
) WHERE district IS NOT NULL;

-- Eski tekil external_id unique index'i kaldırılıp source+external_id kompozit unique'e
-- geçiliyor — Kandilli ve AFAD arasında ID çakışması teorik olarak da imkansız olsun diye
-- (Kandilli client zaten "KANDILLI_" önekiyle sentetik ID üretiyor, ama bu ek bir güvence).
DROP INDEX IF EXISTS idx_earthquake_events_external_id;
CREATE UNIQUE INDEX idx_earthquake_events_source_external_id ON earthquake_events(source, external_id);

ALTER TABLE earthquake_events
    ADD CONSTRAINT chk_earthquake_events_source CHECK (source IN ('AFAD', 'KANDILLI'));

-- Filtreleme sorgu deseni: province + district + tarih sıralaması birlikte kullanılıyor.
CREATE INDEX idx_earthquake_events_province_district_time
    ON earthquake_events(province_normalized, district_normalized, event_time DESC);
CREATE INDEX idx_earthquake_events_source ON earthquake_events(source);
