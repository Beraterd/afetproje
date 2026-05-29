-- =====================================================================
-- V39: Demo/seed toplanma alanı kayıtlarını deaktive et
--
-- Bağlam:
--   V5 ve V19 migrationları demo amaçlı toplanma alanları ekledi.
--   V21, Kadıköy'deki bu kayıtları 'Eski Seed Verisi' olarak işaretledi,
--   ancak diğer ilçelerdeki (Pendik, Fatih, Beşiktaş, Ataşehir vb.) V19
--   kayıtları source_name=NULL ve needs_review=FALSE durumunda kaldı.
--   Bu kayıtlar, gerçek koordinatlara sahip olmayan mock veriler
--   olmalarına rağmen simülasyon mail filtresini geçiyordu.
--
-- Yapılan işlemler:
--   1) source_name=NULL ve needs_review=FALSE olan tüm kayıtlar
--      → source_name='Eski Seed Verisi', needs_review=TRUE, is_active=FALSE
--      (Bunlar: Kurtköy/Pendik, Balat/Fatih, Levent/Beşiktaş,
--               Küçükbakkalköy/Ataşehir, Sultanahmet/Fatih V19 kayıtları)
--
--   2) V21 tarafından zaten işaretlenmiş 'Eski Seed Verisi' kayıtları
--      → is_active=FALSE (needs_review zaten TRUE, ancak is_active=TRUE kalmıştı)
--
-- Sonuç:
--   Artık tüm legacy demo veriler is_active=FALSE; admin panelde
--   varsayılan görünümde çıkmaz, mail dispatch'e dahil edilmez.
--   Gerçek veri için Excel import kullanılmalıdır (POST /api/admin/assembly-areas/import-excel).
--
-- Etkilenmeyenler:
--   - source_name='Kadıköy Belediyesi PDF' kayıtları (V20/V23 gerçek veri)
--   - source_name='İstanbul Toplanma Alanları Master (Excel)' kayıtları (Excel import)
-- =====================================================================

-- 1. V5/V19 demo kayıtlar: Kadıköy dışındakiler V21'de temizlenmedi
UPDATE assembly_areas
SET
    source_name  = 'Eski Seed Verisi',
    needs_review = TRUE,
    is_active    = FALSE
WHERE source_name IS NULL
  AND needs_review = FALSE;

-- 2. V21'de işaretlenmiş ama is_active=TRUE kalan Eski Seed kayıtları
UPDATE assembly_areas
SET is_active = FALSE
WHERE source_name = 'Eski Seed Verisi'
  AND is_active = TRUE;
