package com.afet.koordinasyon.service.ai;

import java.util.List;

public interface AiDamageAssessmentProvider {
    AiAssessmentResult analyze(List<PhotoData> photos) throws Exception;

    default AiAssessmentResult analyzeDescription(String description) throws Exception {
        throw new UnsupportedOperationException("Metin ön değerlendirmesi bu sağlayıcıda desteklenmiyor.");
    }
}
