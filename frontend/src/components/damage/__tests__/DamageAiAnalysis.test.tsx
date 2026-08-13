import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DamageAiAnalysis } from '@/components/damage/DamageAiAnalysis';
import type { DamageAssessmentResponse } from '@/types/damage';
import { AI_DISCLAIMER_TEXT } from '@/components/ai/AiDisclaimer';

function makeAssessment(overrides: Partial<DamageAssessmentResponse> = {}): DamageAssessmentResponse {
    return {
        id: 'd1',
        districtId: 'dist1',
        districtName: 'Kadıköy',
        neighborhoodId: 'n1',
        neighborhoodName: 'Caferağa',
        address: 'Bahariye Cd. 42',
        locationVerified: true,
        damageLevel: 'HEAVY',
        damageLevelLabel: 'Ağır',
        collapseRisk: false,
        emergencyEvacuationNeeded: false,
        casualtiesSuspected: false,
        blockedRoad: false,
        gasLeakRisk: false,
        verificationStatus: 'INCELEME_GEREKIYOR',
        verificationStatusLabel: 'İnceleme Gerekiyor',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
        ...overrides,
    };
}

describe('DamageAiAnalysis', () => {
    it('shows the standard AI disclaimer alongside a completed analysis', () => {
        render(
            <DamageAiAnalysis
                assessment={makeAssessment({
                    aiAnalysisStatus: 'COMPLETED',
                    aiComment: 'Yapısal hasar gözlemlendi.',
                    aiConfidence: 'HIGH',
                    aiConfidenceLabel: 'Yüksek',
                })}
                canRefresh={false}
                refreshing={false}
                onRefresh={() => {}}
            />,
        );
        expect(screen.getByText(AI_DISCLAIMER_TEXT)).toBeInTheDocument();
        expect(screen.getByText('Yüksek')).toBeInTheDocument();
    });

    it('labels confidence as damage clarity/severity, not a generic trust score', () => {
        render(
            <DamageAiAnalysis
                assessment={makeAssessment({
                    aiAnalysisStatus: 'COMPLETED',
                    aiComment: 'Yapısal hasar gözlemlendi.',
                    aiConfidence: 'HIGH',
                    aiConfidenceLabel: 'Yüksek',
                })}
                canRefresh={false}
                refreshing={false}
                onRefresh={() => {}}
            />,
        );
        expect(screen.getByText('Hasar Netliği:')).toBeInTheDocument();
    });

    it('does not render a confidence badge when the backend provides no confidence value', () => {
        render(
            <DamageAiAnalysis
                assessment={makeAssessment({
                    aiAnalysisStatus: 'COMPLETED',
                    aiComment: 'Hafif hasar.',
                    aiConfidence: undefined,
                })}
                canRefresh={false}
                refreshing={false}
                onRefresh={() => {}}
            />,
        );
        expect(screen.queryByText('Hasar Netliği:')).not.toBeInTheDocument();
    });
});
