package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.enums.AiConfidence;
import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.service.ai.AiAssessmentResult;
import com.afet.koordinasyon.service.ai.AiDamageAssessmentProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DamageAiPreviewTest {
    @Test
    void textOnlyPreviewIsConservativeAndDoesNotSaveTheRecord() throws Exception {
        var repository = mock(DamageAssessmentRepository.class);
        var provider = mock(AiDamageAssessmentProvider.class);
        var transactions = mock(PlatformTransactionManager.class);
        var service = new DamageAssessmentAiService(repository, provider, transactions);
        ReflectionTestUtils.setField(service, "aiEnabled", true);
        ReflectionTestUtils.setField(service, "claudeApiKey", "test-key");
        var assessment = new DamageAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setDamageLevel(DamageLevel.LIGHT);
        assessment.setPhotos(List.of());
        when(repository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(provider.analyzeDescription(anyString())).thenReturn(
                new AiAssessmentResult("Saha incelemesi önerilir.", AiConfidence.HIGH, "test-model", 1, "İnceleme", 90));
        var result = service.preview(assessment.getId());
        assertThat(result.comment()).contains("yalnızca bildirim metnine");
        assertThat(result.confidence()).isEqualTo(AiConfidence.LOW);
        assertThat(result.riskScore()).isNull();
        assertThat(result.priority()).isNull();
        verify(repository, never()).save(any());
        verify(provider, never()).analyze(any());
        verify(transactions).getTransaction(argThat(definition -> definition.isReadOnly()));
    }
}
