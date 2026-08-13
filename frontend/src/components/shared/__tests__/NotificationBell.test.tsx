import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { NotificationBell } from '@/components/shared/NotificationBell';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse, NotificationResponse, PagedResponse, UnreadCountResponse } from '@/types';

vi.mock('@/api/notifications.api', () => ({
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAllNotificationsRead: vi.fn(),
    markNotificationRead: vi.fn(),
}));

import { getNotifications, getUnreadCount, markNotificationRead } from '@/api/notifications.api';

function LocationDisplay() {
    const location = useLocation();
    return <div data-testid="location">{location.pathname}{location.search}</div>;
}

function renderBell() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/dashboard']}>
                <NotificationBell />
                <LocationDisplay />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

const damageNotification: NotificationResponse = {
    id: 'notif-1',
    type: 'DAMAGE_REPORT',
    title: 'Yeni ağır hasar kaydı',
    message: 'Bir kayıt incelemenizi bekliyor',
    relatedEntityType: 'DamageAssessment',
    relatedEntityId: 'dmg-1',
    isRead: false,
    createdAt: new Date().toISOString(),
};

const taskNotification: NotificationResponse = {
    id: 'notif-2',
    type: 'TEAM_NEED',
    title: 'Ekip ihtiyacı doğdu',
    relatedEntityType: 'Event',
    relatedEntityId: 'evt-9',
    isRead: false,
    createdAt: new Date().toISOString(),
};

function pagedResponse(items: NotificationResponse[]): PagedResponse<NotificationResponse> {
    return {
        content: items, totalElements: items.length, totalPages: 1, size: 20,
        number: 0, last: true, first: true, empty: items.length === 0,
    };
}

beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
        accessToken: 'token',
        user: { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role: 'ADMIN' } as UserSummaryResponse,
    });
    (getUnreadCount as any).mockResolvedValue({ unreadCount: 2 } as UnreadCountResponse);
    (markNotificationRead as any).mockResolvedValue(undefined);
});

describe('NotificationBell deep-link navigation', () => {
    it('clicking a DAMAGE type notification marks it read and navigates to the damage assessment', async () => {
        (getNotifications as any).mockResolvedValue(pagedResponse([damageNotification]));
        renderBell();
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: /bildirimler/i }));
        await screen.findByText('Yeni ağır hasar kaydı');

        await user.click(screen.getByText('Yeni ağır hasar kaydı'));

        await waitFor(() => expect(markNotificationRead).toHaveBeenCalled());
        expect((markNotificationRead as any).mock.calls[0][0]).toBe('notif-1');
        await waitFor(() =>
            expect(screen.getByTestId('location').textContent).toBe('/damage-assessments?selected=dmg-1'),
        );
    });

    it('clicking a TEAM_NEED (event) notification navigates to the event detail route', async () => {
        (getNotifications as any).mockResolvedValue(pagedResponse([taskNotification]));
        renderBell();
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: /bildirimler/i }));
        await screen.findByText('Ekip ihtiyacı doğdu');
        await user.click(screen.getByText('Ekip ihtiyacı doğdu'));

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/events/evt-9'));
    });

    it('navigation still happens even when mark-as-read fails', async () => {
        (getNotifications as any).mockResolvedValue(pagedResponse([damageNotification]));
        (markNotificationRead as any).mockRejectedValue(new Error('network error'));
        renderBell();
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: /bildirimler/i }));
        await screen.findByText('Yeni ağır hasar kaydı');
        await user.click(screen.getByText('Yeni ağır hasar kaydı'));

        await waitFor(() =>
            expect(screen.getByTestId('location').textContent).toBe('/damage-assessments?selected=dmg-1'),
        );
    });
});
