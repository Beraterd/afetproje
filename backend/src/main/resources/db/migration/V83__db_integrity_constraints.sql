-- item 47: DB seviyesinde invariant'ları garanti altına al (service validation tek başına
-- race condition'a açık). Constraint eklemeden ÖNCE mevcut veriyle çakışma riskini gideriyoruz:
-- negatif stok değerleri varsa 0'a çekilir (silme değil, düzeltme), aynı hasar tespitine
-- birden fazla AKTİF atama varsa yalnızca en yenisi aktif bırakılır (geçmiş kayıtlar silinmez,
-- yalnızca is_active=false yapılır — soft-delete modeline zaten uygun).

-- 1) Stok miktarı ve kritik eşik negatif olamaz.
UPDATE resource_stocks SET quantity = 0 WHERE quantity < 0;
UPDATE resource_stocks SET critical_threshold = 0 WHERE critical_threshold < 0;

ALTER TABLE resource_stocks
    ADD CONSTRAINT chk_resource_stocks_quantity_non_negative CHECK (quantity >= 0);
ALTER TABLE resource_stocks
    ADD CONSTRAINT chk_resource_stocks_critical_threshold_non_negative CHECK (critical_threshold >= 0);

-- 2) Aynı hasar tespitine aynı kullanıcı için ikinci bir AKTİF atama olamaz
--    (EventAssignment'ta zaten var olan uq_event_assignment ile aynı sınıf invariant).
--    Önce olası mevcut duplicate'leri temizle: her (damage_assessment_id, user_id) çifti için
--    yalnızca en son assigned_at'e sahip satır aktif kalsın.
UPDATE damage_assessment_assignments daa
SET is_active = FALSE,
    removed_at = COALESCE(daa.removed_at, now()),
    updated_at = now()
WHERE daa.is_active = TRUE
  AND daa.id <> (
      SELECT newer.id
      FROM damage_assessment_assignments newer
      WHERE newer.damage_assessment_id = daa.damage_assessment_id
        AND newer.user_id = daa.user_id
        AND newer.is_active = TRUE
      ORDER BY newer.assigned_at DESC, newer.id DESC
      LIMIT 1
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_daa_active_assignment
    ON damage_assessment_assignments (damage_assessment_id, user_id)
    WHERE is_active = TRUE;
