import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EventAiRecommendationPanel } from '@/components/events/EventAiRecommendationPanel';
import type { TeamRecommendationResponse } from '@/types';
import { AI_DISCLAIMER_TEXT } from '@/components/ai/AiDisclaimer';

function makeRecommendation(): TeamRecommendationResponse {
    return {
        id: 'rec1',
        eventId: 'ev1',
        teamType: 'SEARCH_RESCUE',
        teamTypeLabel: 'Arama Kurtarma',
        districtName: 'Kadıköy',
        requiredTeamSize: 2,
        priority: 'HIGH',
        status: 'DRAFT',
        aiExplanation: 'Geçmiş görev performansına göre öneriliyor.',
        recommendedPersonnel: [
            {
                userId: 'u1',
                firstName: 'Ada',
                lastName: 'Lovelace',
                email: 'ada@example.com',
                score: 82,
                orderId: 1,
                completedSimilarTasks: 3,
                reasons: ['Olay yerine mesafe: 1.8 km', 'Ekip durumu: Müsait'],
                proximityLevel: 'NEAR',
                availability: 'AVAILABLE',
            },
        ],
        requestedByName: 'Test Coordinator',
        createdAt: '2026-01-01T00:00:00Z',
    };
}

const baseProps = {
    autoFetching: false,
    recommendPending: false,
    approvePending: false,
    rejectPending: false,
    selectedUserIds: new Set<string>(),
    requiredPeople: 2,
    onToggleSelect: vi.fn(),
    onRequestRecommendation: vi.fn(),
    onClearRecommendation: vi.fn(),
    onApprove: vi.fn(),
    onReject: vi.fn(),
};

describe('EventAiRecommendationPanel', () => {
    it('shows the AI disclaimer', () => {
        render(<EventAiRecommendationPanel recommendation={makeRecommendation()} {...baseProps} />);
        expect(screen.getByText(AI_DISCLAIMER_TEXT)).toBeInTheDocument();
    });

    it('reveals real backend-provided reasons behind "Neden bu öneri?"', async () => {
        render(<EventAiRecommendationPanel recommendation={makeRecommendation()} {...baseProps} />);
        const user = userEvent.setup();

        expect(screen.queryByText(/Olay yerine mesafe/)).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: /neden bu öneri/i }));
        expect(screen.getByText('• Olay yerine mesafe: 1.8 km')).toBeInTheDocument();
        expect(screen.getByText('• Ekip durumu: Müsait')).toBeInTheDocument();
    });

    it('does not call onApprove merely by rendering the recommendation — approval requires an explicit click', () => {
        const onApprove = vi.fn();
        render(<EventAiRecommendationPanel recommendation={makeRecommendation()} {...baseProps} onApprove={onApprove} />);
        expect(onApprove).not.toHaveBeenCalled();
    });
});
