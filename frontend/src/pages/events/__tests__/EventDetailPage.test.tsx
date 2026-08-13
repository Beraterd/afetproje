import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { ToastProvider } from '@/components/shared/ToastProvider';
import type { UserSummaryResponse, EventResponse } from '@/types';

vi.mock('@/api/events.api', () => ({
    getEventById: vi.fn(),
    joinEvent: vi.fn(),
    leaveEvent: vi.fn(),
    closeEvent: vi.fn(),
    completeEvent: vi.fn(),
    updateEvent: vi.fn(),
    getEventParticipants: vi.fn().mockResolvedValue([]),
    getEventTimeline: vi.fn().mockResolvedValue([]),
}));
vi.mock('@/api/teamRecommendations.api', () => ({
    createEventTeamRecommendation: vi.fn(),
    getLatestEventTeamRecommendation: vi.fn().mockResolvedValue(null),
    approveTeamRecommendation: vi.fn(),
    rejectTeamRecommendation: vi.fn(),
}));

import { getEventById } from '@/api/events.api';
import { EventDetailPage } from '@/pages/events/EventDetailPage';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role };
}

const sampleEvent: EventResponse = {
    id: 'evt-1',
    title: 'Kadıköy Bina Çökmesi',
    description: 'Test açıklaması',
    status: 'OPEN',
    requiredPeople: 5,
    riskScore: 3,
    team: { id: 'team-1', name: 'SEARCH_RESCUE', teamCode: 'KAD-AK-1' } as any,
    neighborhood: { id: 'n1', name: 'Moda', districtId: 'd1', districtName: 'Kadıköy' } as any,
    createdBy: { id: 'creator-1', firstName: 'Yönetici', lastName: 'Hesap', email: 'y@b.com', role: 'ADMIN' } as any,
    startsAt: new Date().toISOString(),
    endsAt: new Date().toISOString(),
    assignedVolunteers: 1,
    acceptedCount: 1,
    isParticipating: false,
    currentUserJoined: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    eventTeamCode: 'KAD-AK-1',
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <ToastProvider>
                <MemoryRouter initialEntries={['/events/evt-1']}>
                    <Routes>
                        <Route path="/events/:id" element={<EventDetailPage />} />
                    </Routes>
                </MemoryRouter>
            </ToastProvider>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    (getEventById as any).mockResolvedValue(sampleEvent);
    useAuthStore.setState({ accessToken: 't', user: makeUser('ADMIN') });
});

describe('EventDetailPage (post-refactor)', () => {
    it('renders the header card (title, status, location, team)', async () => {
        renderPage();
        expect(await screen.findByText('Kadıköy Bina Çökmesi')).toBeInTheDocument();
        expect(screen.getByText(/Moda/)).toBeInTheDocument();
    });

    it('shows management actions for a manager role (canManage)', async () => {
        renderPage();
        await screen.findByText('Kadıköy Bina Çökmesi');
        expect(screen.getByRole('button', { name: /düzenle/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'İptal Et' })).toBeInTheDocument();
    });

    it('shows the join button (not management actions) for a volunteer', async () => {
        useAuthStore.setState({ accessToken: 't', user: makeUser('VOLUNTEER') });
        renderPage();
        await screen.findByText('Kadıköy Bina Çökmesi');
        expect(screen.getByRole('button', { name: /^katıl$/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /düzenle/i })).not.toBeInTheDocument();
    });
});
