-- V12: Backfill auto-generated event titles from English enum format to Turkish team names.
-- Only titles matching the old auto-generated pattern are updated.
-- Manually entered titles (not matching the enum-name prefix) are left untouched.

UPDATE events e
SET title =
    CASE t.name::text
        WHEN 'SEARCH_RESCUE' THEN 'Arama Kurtarma Ekibi'
        WHEN 'FOOD_WATER'    THEN 'Yemek ve İçme Suyu Dağıtım Ekibi'
        WHEN 'EVACUATION'    THEN 'Tahliye Ekibi'
        WHEN 'COMMUNICATION' THEN 'İletişim Ekibi'
        WHEN 'PSYCHOSOCIAL'  THEN 'Psikososyal Destek Ekibi'
        WHEN 'OTHER'         THEN 'Diğer'
        ELSE t.name::text
    END || ' - ' || n.name
FROM teams t, neighborhoods n
WHERE e.team_id = t.id
  AND e.neighborhood_id = n.id
  AND (
      e.title LIKE 'SEARCH RESCUE - %'
   OR e.title LIKE 'FOOD WATER - %'
   OR e.title LIKE 'EVACUATION - %'
   OR e.title LIKE 'COMMUNICATION - %'
   OR e.title LIKE 'PSYCHOSOCIAL - %'
   OR e.title LIKE 'OTHER - %'
  );
