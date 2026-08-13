import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { ToastProvider } from '@/components/shared/ToastProvider';
import { queryKeys } from '@/utils/queryKeys';
import type { UserSummaryResponse, DamageAssessmentResponse, PagedResponse } from '@/types';

vi.mock('@/api/damageAssessments.api', () => ({
    getDamageAssessments: vi.fn(),
    getDamageAssessmentById: vi.fn(),
    createDamageAssessment: vi.fn(),
    verifyDamageAssessment: vi.fn(),
    assignDamageAssessment: vi.fn(),
    removeDamageAssignment: vi.fn(),
    getEligibleAssignees: vi.fn(),
    triggerAiAnalysis: vi.fn(),
    enqueueMissingAiAnalysis: vi.fn().mockResolvedValue({ enqueued: 0 }),
}));
vi.mock('@/api/districts.api', () => ({ getDistricts: vi.fn().mockResolvedValue([]) }));
vi.mock('@/api/neighborhoods.api', () => ({ getNeighborhoods: vi.fn().mockResolvedValue([]) }));

import { getDamageAssessments, verifyDamageAssessment } from '@/api/damageAssessments.api';
import { DamageAssessmentsPage } from '@/pages/damage/DamageAssessmentsPage';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role };
}

function paged(content: DamageAssessmentResponse[]): PagedResponse<DamageAssessmentResponse> {
    return { content, totalElements: content.length, totalPages: 1, size: 15, number: 0, last: true, first: true, empty: content.length === 0 };
}

const sample: DamageAssessmentResponse = {
    id: 'dmg-1',
    districtId: 'd1', districtName: 'Kadıköy',
    neighborhoodId: 'n1', neighborhoodName: 'Moda',
    buildingId: 'bldg-1',
    address: 'Test Sokak No:1',
    locationVerified: true,
    damageLevel: 'HEAVY', damageLevelLabel: 'Ağır',
    collapseRisk: false, emergencyEvacuationNeeded: false, casualtiesSuspected: false, blockedRoad: false, gasLeakRisk: false,
    verificationStatus: 'SAHADA_DOGRULANDI', verificationStatusLabel: 'Sahada Doğrulandı',
    reportedBy: 'Test Kullanıcı',
    createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
};

function renderPage(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
    return render(
        <QueryClientProvider client={queryClient}>
            <ToastProvider>
                <MemoryRouter initialEntries={['/damage-assessments']}>
                    <DamageAssessmentsPage />
                </MemoryRouter>
            </ToastProvider>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    (getDamageAssessments as any).mockResolvedValue(paged([sample]));
    useAuthStore.setState({ accessToken: 't', user: makeUser('ADMIN') });
});

describe('DamageAssessmentsPage (post-refactor)', () => {
    it('renders the page heading and the create button for a user allowed to create', async () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'Hasar Tespiti' })).toBeInTheDocument();
        expect(await screen.findByRole('button', { name: /yeni tespit/i })).toBeInTheDocument();
    });

    it('renders the fetched list rows (DamageAssessmentList)', async () => {
        renderPage();
        expect(await screen.findByText('Test Sokak No:1')).toBeInTheDocument();
        expect(screen.getByText('Ağır')).toBeInTheDocument();
    });

    it('opens the detail modal (DamageAssessmentDetail) when the row detail button is clicked', async () => {
        renderPage();
        await screen.findByText('Test Sokak No:1');

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /detayları gör/i }));

        expect(await screen.findByText('Hasar Tespiti Detayı')).toBeInTheDocument();
        expect(screen.getAllByText('Test Kullanıcı').length).toBeGreaterThan(0);
    });

    it('opens the verify modal (DamageAssessmentVerification) from the row action', async () => {
        renderPage();
        await screen.findByText('Test Sokak No:1');

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /durumu güncelle/i }));

        expect(await screen.findByText('Doğrulama Durumunu Güncelle')).toBeInTheDocument();
    });

    it('invalidates the building/map queries after a successful verification (item 23: map color must refresh)', async () => {
        (verifyDamageAssessment as any).mockResolvedValue({ ...sample, verificationStatus: 'KOORDINATOR_ONAYLADI' });
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
        renderPage(queryClient);
        await screen.findByText('Test Sokak No:1');

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /durumu güncelle/i }));
        await screen.findByText('Doğrulama Durumunu Güncelle');
        await user.click(screen.getByRole('button', { name: 'Güncelle' }));

        await waitFor(() => expect(verifyDamageAssessment).toHaveBeenCalled());
        expect(invalidateSpy).toHaveBeenCalledWith(
            expect.objectContaining({ queryKey: queryKeys.buildings.detail('bldg-1') }),
        );
        expect(invalidateSpy).toHaveBeenCalledWith(
            expect.objectContaining({ queryKey: queryKeys.map.damageSummary() }),
        );
        expect(invalidateSpy).toHaveBeenCalledWith(
            expect.objectContaining({ queryKey: ['buildings', 'bbox'] }),
        );
    });
});
