-- =====================================================================
-- V21: Kadıköy veri kaynağı temizliği
--
-- Sorun:
--   V19 migrationı Kadıköy için iki test/seed kaydı ekledi (source_name=NULL).
--   V20 needs_review=FALSE DEFAULT ile bu kayıtlar mail filtresini geçiyordu.
--   Sonuç: Bostancı için mailde 3 kayıt çıkıyordu (2 eski seed + 1 PDF).
--
-- Çözüm:
--   Kadıköy ilçesindeki tüm kayıtlarda birincil kaynak kontrolü yap.
--   PDF olmayan kayıtları needs_review=TRUE + 'Eski Seed Verisi' olarak işaretle.
--   Mail filtresi (needs_review=FALSE + source_name='Kadıköy Belediyesi PDF') artık
--   yalnızca doğrulanmış PDF kayıtlarını gösterecek.
--
-- Etkilenen Kadıköy kayıtları (source_name=NULL, V5/V19 seed):
--   - Bostancı Sahil Bandı Toplanma Alanı      (V19)
--   - Bostancı Metro İstasyonu Çıkışı           (V19)
--   - Moda Koşu Yolu Toplanma                  (V5)
--   - Fenerbahçe Parkı Toplanma Alanı           (V19)
--
-- Diğer ilçeler (Pendik, Bakırköy, vb.) ETKİLENMEZ.
-- =====================================================================

-- is_active sütununu ekle (yeni kayıtlar için açık kapı, şimdilik hepsi TRUE)
ALTER TABLE assembly_areas
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Kadıköy'deki PDF dışı kayıtları işaretle
UPDATE assembly_areas
SET
    needs_review = TRUE,
    source_name  = COALESCE(NULLIF(TRIM(source_name), ''), 'Eski Seed Verisi')
WHERE
    district_id = '11111111-1111-1111-1111-000000000004'   -- Kadıköy
    AND COALESCE(source_name, '') != 'Kadıköy Belediyesi PDF';

-- =====================================================================
-- Doğrulama: Bostancı mahallesi kayıt durumu
--
-- Beklenen sonuç:
--   source_name='Kadıköy Belediyesi PDF' + needs_review=FALSE  → 1  (BOSTANCI GÖSTERİ MERKEZİ)
--   source_name='Kadıköy Belediyesi PDF' + needs_review=TRUE   → 14 (koordinatsız PDF kayıtlar)
--   source_name='Eski Seed Verisi'       + needs_review=TRUE   → 2  (V19 seed, artık dışlanmış)
--   Mailde görünen: 1
-- =====================================================================
