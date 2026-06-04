-- =====================================================================
-- V70: Mevcut users.phone değerlerini E.164 (+905XXXXXXXXX) formatına normalize et.
--   0539...   -> +90539...
--   539...    -> +90539...
--   90539...  -> +90539...
--   +90539... -> zaten doğru, dokunulmaz
-- Normalize edilemeyen (geçersiz / TR cep olmayan) numaralar DEĞİŞTİRİLMEZ;
-- çakışma yaratacak normalizasyonlar da atlanır. Tümü RAISE NOTICE ile raporlanır.
-- =====================================================================

DO $$
DECLARE
    r           RECORD;
    digits      TEXT;
    normalized  TEXT;
    updated_cnt INT := 0;
    skipped_cnt INT := 0;
BEGIN
    FOR r IN SELECT id, phone FROM users WHERE phone IS NOT NULL AND phone <> '' LOOP
        -- Yalnızca rakamları al
        digits := regexp_replace(r.phone, '[^0-9]', '', 'g');
        normalized := NULL;

        IF length(digits) = 12 AND left(digits, 2) = '90' AND substr(digits, 3, 1) = '5' THEN
            normalized := '+' || digits;
        ELSIF length(digits) = 11 AND left(digits, 1) = '0' AND substr(digits, 2, 1) = '5' THEN
            normalized := '+9' || digits;
        ELSIF length(digits) = 10 AND left(digits, 1) = '5' THEN
            normalized := '+90' || digits;
        END IF;

        -- Normalize edilemeyenleri olduğu gibi bırak, raporla
        IF normalized IS NULL THEN
            RAISE NOTICE 'V70: phone normalize EDILEMEDI (degistirilmedi): userId=%, phone=%', r.id, r.phone;
            skipped_cnt := skipped_cnt + 1;
            CONTINUE;
        END IF;

        -- Zaten doğru formatta ise atla
        IF normalized = r.phone THEN
            CONTINUE;
        END IF;

        -- Unique kısıtını ihlal edecek çakışmaları atla, raporla
        IF EXISTS (SELECT 1 FROM users u2 WHERE u2.phone = normalized AND u2.id <> r.id) THEN
            RAISE NOTICE 'V70: phone normalize CAKISMASI (degistirilmedi): userId=%, % -> %', r.id, r.phone, normalized;
            skipped_cnt := skipped_cnt + 1;
            CONTINUE;
        END IF;

        UPDATE users SET phone = normalized WHERE id = r.id;
        updated_cnt := updated_cnt + 1;
    END LOOP;

    RAISE NOTICE 'V70: Telefon normalizasyonu tamamlandi. guncellenen=%, atlanan=%', updated_cnt, skipped_cnt;
END $$;
