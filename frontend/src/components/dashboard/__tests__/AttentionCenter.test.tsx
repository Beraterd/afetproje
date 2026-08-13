import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AttentionCenter } from '@/components/dashboard/AttentionCenter';
import type { PagedResponse } from '@/types';

vi.mock('@/api/teamRecommendations.api', () => ({ getActiveEventCount: vi.fn() }));
vi.mock('@/api/damageAssessments.api', () => ({ getDamageAssessments: vi.fn() }));
vi.mock('@/api/resourceRequests.api', () => ({ getResourceRequests: vi.fn() }));
vi.mock('@/api/documents.api', () => ({ getPendingDocuments: vi.fn() }));
vi.mock('@/api/resourceStocks.api', () => ({ getStockSummary: vi.fn() }));

import { getActiveEventCount } from '@/api/teamRecommendations.api';
import { getDamageAssessments } from '@/api/damageAssessments.api';
import { getResourceRequests } from '@/api/resourceRequests.api';
import { getPendingDocuments } from '@/api/documents.api';
import { getStockSummary } from '@/api/resourceStocks.api';

function paged<T>(count: number): PagedResponse<T> {
    return { content: [], totalElements: count, totalPages: 1, size: 1, number: 0, last: true, first: true, empty: count === 0 };
}

function LocationDisplay() {
    const location = useLocation();
    return <div data-testid="location">{location.pathname}{location.search}</div>;
}

function renderAttentionCenter() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/dashboard']}>
                <AttentionCenter />
                <LocationDisplay />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    (getPendingDocuments as any).mockResolvedValue(paged(0));
    (getStockSummary as any).mockResolvedValue({ totalItems: 10, criticalCount: 0, outOfStockCount: 0 });
    (getResourceRequests as any).mockResolvedValue(paged(0));
});

describe('AttentionCenter', () => {
    it('shows a calm empty-state message when nothing needs attention', async () => {
        (getActiveEventCount as any).mockResolvedValue(0);
        (getDamageAssessments as any).mockResolvedValue(paged(0));
        renderAttentionCenter();

        await screen.findByText(/dikkat gerektiren kritik bir durum bulunmuyor/i);
    });

    it('renders the open-events item with CRITICAL severity', async () => {
        (getActiveEventCount as any).mockResolvedValue(4);
        (getDamageAssessments as any).mockResolvedValue(paged(0));
        renderAttentionCenter();

        await screen.findByText(/4 açık ekip ihtiyacı karşılanmayı bekliyor/i);
        expect(screen.getByText('Kritik')).toBeInTheDocument();
    });

    it('clicking a damage-approval item navigates with the correct filter query params', async () => {
        (getActiveEventCount as any).mockResolvedValue(0);
        (getDamageAssessments as any).mockImplementation((params: any) =>
            Promise.resolve(paged(params.damageLevel === 'HEAVY' ? 2 : 1)),
        );
        renderAttentionCenter();

        const link = await screen.findByText(/ağır hasar kaydı koordinatör onayı bekliyor/i);
        const user = userEvent.setup();
        await user.click(link);

        await waitFor(() =>
            expect(screen.getByTestId('location').textContent).toBe(
                '/damage-assessments?verificationStatus=SAHADA_DOGRULANDI&damageLevel=HEAVY',
            ),
        );
    });

    it('does not blow up the section when one data source fails — others still render', async () => {
        (getActiveEventCount as any).mockRejectedValue(new Error('network error'));
        (getDamageAssessments as any).mockResolvedValue(paged(0));
        (getStockSummary as any).mockResolvedValue({ totalItems: 5, criticalCount: 1, outOfStockCount: 0 });
        renderAttentionCenter();

        await screen.findByText(/kritik veya tükenmiş stok seviyesi var/i);
    });
});
