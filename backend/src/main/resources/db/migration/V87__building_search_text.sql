-- V87: Türkçe-katlanmış, sokak/cadde/mahalle kısaltmalarından arındırılmış arama metni sütunu.
-- BuildingImportService her satırda bu sütunu yazar (bkz. TurkishTextNormalizer) — "Selvi Boylum
-- Sk" ve "Selvi Boylum Sokak" gibi varyantların aynı sonucu döndürmesi için. display_address
-- üzerindeki eski trigram index artık kullanılmıyor (BuildingRepository.searchByDistrict search_text'e
-- taşındı); yerine search_text için yeni bir trigram index eklenir.
ALTER TABLE buildings ADD COLUMN search_text TEXT;

DROP INDEX IF EXISTS idx_buildings_display_address_trgm;

CREATE INDEX idx_buildings_search_text_trgm ON buildings USING gin (search_text gin_trgm_ops);
