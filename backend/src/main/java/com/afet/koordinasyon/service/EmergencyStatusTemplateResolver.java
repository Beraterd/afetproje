package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.enums.EmergencyStatusTemplateKey;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves Turkish message text for each EmergencyStatusTemplateKey.
 * Single source of truth — both the send flow and the templates endpoint use this.
 */
@Component
public class EmergencyStatusTemplateResolver {

    private static final Map<EmergencyStatusTemplateKey, String> MESSAGES = new LinkedHashMap<>();

    static {
        MESSAGES.put(EmergencyStatusTemplateKey.SAFE_AND_WELL,
                "Şu anda güvendeyim, sağlık durumum iyi.");
        MESSAGES.put(EmergencyStatusTemplateKey.AREA_UNCERTAIN,
                "Şu anda güvendeyim ancak bulunduğum bölgede durum belirsiz.");
        MESSAGES.put(EmergencyStatusTemplateKey.OUTSIDE_AND_SAFE,
                "Evden çıktım, dışarıdayım ve şu an iyiyim.");
        MESSAGES.put(EmergencyStatusTemplateKey.HOME_DAMAGED_BUT_SAFE,
                "Evimde hasar var ancak ben güvendeyim.");
        MESSAGES.put(EmergencyStatusTemplateKey.NEED_HELP,
                "Bulunduğum yerde yardıma ihtiyaç var.");
        MESSAGES.put(EmergencyStatusTemplateKey.TRAPPED_UNDER_RUBBLE,
                "Göçük altında veya mahsur kalmış olabilirim. Acil yardım gerekiyor.");
    }

    public String resolve(EmergencyStatusTemplateKey key) {
        return MESSAGES.get(key);
    }

    /** Returns all templates as an ordered map of key → message text. */
    public Map<EmergencyStatusTemplateKey, String> allTemplates() {
        return MESSAGES;
    }
}
