-- Her sync'te AFAD API raw response pozisyonunu saklar (0-based).
-- Her sync başında NULL'a sıfırlanır, sonra son 200 event için set edilir.
-- NULL = bu event son AFAD batch'inde yoktu (eski kayıt).
ALTER TABLE earthquake_events
    ADD COLUMN afad_order_index INTEGER;

CREATE INDEX idx_earthquake_events_afad_order
    ON earthquake_events (afad_order_index NULLS LAST);
