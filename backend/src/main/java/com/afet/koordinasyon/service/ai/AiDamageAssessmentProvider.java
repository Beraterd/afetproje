package com.afet.koordinasyon.service.ai;

import java.util.List;

public interface AiDamageAssessmentProvider {
    AiAssessmentResult analyze(List<PhotoData> photos) throws Exception;
}
