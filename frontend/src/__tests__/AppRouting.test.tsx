import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

// App.tsx tüm sayfaları statik import ediyor — canonical router tek App.tsx içinde
// tanımlı (src/router/index.tsx kaldırıldı). Bu test dosyası doğrudan App'i MemoryRouter
// içinde render ederek gerçek route eşleşmesini doğrular.
vi.mock('@/api/events.api', () => ({
    getMyEvents: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, size: 50, number: 0, last: true, first: true, empty: true }),
    leaveEvent: vi.fn(),
    getEvents: vi.fn(),
    getEventById: vi.fn(),
    joinEvent: vi.fn(),
    closeEvent: vi.fn(),
    completeEvent: vi.fn(),
    updateEvent: vi.fn(),
    getEventParticipants: vi.fn(),
}));
vi.mock('@/api/myTasks.api', () => ({
    getMyDamageAssessmentTasks: vi.fn().mockResolvedValue([]),
    uploadFieldPhotos: vi.fn(),
    markFieldVerified: vi.fn(),
    completeAssessmentTask: vi.fn(),
}));
vi.mock('@/api/damageAssessments.api', () => ({
    getDamageAssessments: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, size: 15, number: 0, last: true, first: true, empty: true }),
    getDamageAssessmentById: vi.fn().mockResolvedValue({ id: 'dmg-1', address: 'Test adres', damageLevel: 'HEAVY', damageLevelLabel: 'Ağır', verificationStatus: 'SAHADA_DOGRULANDI', verificationStatusLabel: 'Sahada Doğrulandı', neighborhoodName: 'Moda', districtName: 'Kadıköy', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), locationVerified: true, collapseRisk: false, emergencyEvacuationNeeded: false, casualtiesSuspected: false, blockedRoad: false, gasLeakRisk: false }),
    createDamageAssessment: vi.fn(),
    verifyDamageAssessment: vi.fn(),
    assignDamageAssessment: vi.fn(),
    removeDamageAssignment: vi.fn(),
    getEligibleAssignees: vi.fn(),
    triggerAiAnalysis: vi.fn(),
    enqueueMissingAiAnalysis: vi.fn().mockResolvedValue({ enqueued: 0 }),
}));

import App from '@/App';
import { getDamageAssessmentById } from '@/api/damageAssessments.api';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role };
}

function renderAppAt(initialRoute: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialRoute]}>
                <App />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ accessToken: null, user: null });
    localStorage.clear();
});

describe('App routing — canonical router (single source in App.tsx)', () => {
    it('renders the "Görevlerim" (MyTasksPage) route for an authenticated user', async () => {
        useAuthStore.setState({ accessToken: 't', user: makeUser('VOLUNTEER') });
        renderAppAt('/my-tasks');

        expect(await screen.findByRole('heading', { name: 'Görevlerim' })).toBeInTheDocument();
    });

    it('redirects an anonymous user away from a protected route to /login', async () => {
        renderAppAt('/damage-assessments');

        expect(await screen.findByRole('heading', { name: /giriş/i })).toBeInTheDocument();
    });

    it('renders the 404 page for an unmatched route', async () => {
        useAuthStore.setState({ accessToken: 't', user: makeUser('ADMIN') });
        renderAppAt('/this-route-does-not-exist');

        expect(await screen.findByText('Sayfa Bulunamadı')).toBeInTheDocument();
    });

    it('honors the ?selected= notification deep-link on /damage-assessments', async () => {
        useAuthStore.setState({ accessToken: 't', user: makeUser('ADMIN') });
        renderAppAt('/damage-assessments?selected=dmg-1');

        await screen.findByRole('heading', { name: 'Hasar Tespiti' });
        expect(getDamageAssessmentById).toHaveBeenCalledWith('dmg-1');
        expect(await screen.findByText('Hasar Tespiti Detayı')).toBeInTheDocument();
    });
});
