-- V86: Optional link from a damage report to a real Building. Nullable so existing rows
-- (and every future non-building-linked report) keep working unmodified.
ALTER TABLE damage_assessments ADD COLUMN building_id UUID REFERENCES buildings(id) ON DELETE SET NULL;
CREATE INDEX idx_damage_assessments_building_id ON damage_assessments(building_id);
