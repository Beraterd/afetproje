-- V56: OPEN durumundaki event kayıtlarını IN_PROGRESS'e çevir.
-- Artık yeni event'ler direkt IN_PROGRESS olarak oluşturuluyor (Event.java @Builder.Default).
-- Bu migration legacy OPEN kayıtları temizler; OPEN enum değeri Hibernate mapping
-- güvenliği için enum'da bırakılır ama UI ve create flow artık kullanmaz.

UPDATE events
   SET status     = 'IN_PROGRESS',
       updated_at = now()
 WHERE status = 'OPEN';

DO $$
DECLARE
  v_remaining INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_remaining FROM events WHERE status = 'OPEN';
  IF v_remaining > 0 THEN
    RAISE EXCEPTION 'V56 HATA: % event hâlâ OPEN durumunda', v_remaining;
  END IF;
  RAISE NOTICE 'V56 başarılı: tüm OPEN eventler IN_PROGRESS yapıldı';
END $$;
