-- V67: Yeni ekip kodu formatı: [DIST_PREFIX]-[TYPE_CODE]-[SEQ]
-- Eski global ekipler: SAR-1, FW-1, EVA-1, COM-1, PSY-1, HTK-1, LOG-1, OTH-1
-- Yeni format       : IST-AKU-1, IST-YEM-1, IST-TAH-1, IST-ILE-1, IST-PSK-1, IST-HAT-1, IST-LOJ-1, IST-DIG-1

-- ── 1. Global ekipler (district_id NULL) — eski koda göre güncelle ──────────

UPDATE teams
SET team_code = CASE name::text
    WHEN 'SEARCH_RESCUE'       THEN 'IST-AKU-1'
    WHEN 'FOOD_WATER'          THEN 'IST-YEM-1'
    WHEN 'EVACUATION'          THEN 'IST-TAH-1'
    WHEN 'COMMUNICATION'       THEN 'IST-ILE-1'
    WHEN 'PSYCHOSOCIAL'        THEN 'IST-PSK-1'
    WHEN 'HASAR_TESPIT_EKIBI'  THEN 'IST-HAT-1'
    WHEN 'LOGISTICS'           THEN 'IST-LOJ-1'
    WHEN 'OTHER'               THEN 'IST-DIG-1'
    ELSE team_code
END
WHERE district_id IS NULL
  AND team_code ~ '^[A-Z]{2,5}-[0-9]+$';

-- ── 2. İlçeye bağlı ekipler — eski 2 parçalı format varsa NULL yap (backfill yeniden yapar) ──

UPDATE teams
SET team_code = NULL
WHERE district_id IS NOT NULL
  AND team_code ~ '^[A-Z]{2,5}-[0-9]+$';

-- ── 3. Unique index — zaten V65'te var; yeniden oluşturma ──────────────────

CREATE UNIQUE INDEX IF NOT EXISTS uniq_teams_team_code_v2
    ON teams (team_code)
    WHERE team_code IS NOT NULL;
