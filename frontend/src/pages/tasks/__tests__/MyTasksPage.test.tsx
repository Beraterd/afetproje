import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { UserEventResponse, DamageAssessmentTaskResponse } from '@/types';
import { ToastProvider } from '@/components/shared/ToastProvider';

vi.mock('@/api/events.api', () => ({
    getMyEvents: vi.fn(),
    leaveEvent: vi.fn(),
}));
vi.mock('@/api/myTasks.api', () => ({
    getMyDamageAssessmentTasks: vi.fn(),
    uploadFieldPhotos: vi.fn(),
    markFieldVerified: vi.fn(),
    completeAssessmentTask: vi.fn(),
}));

import { getMyEvents } from '@/api/events.api';
import { getMyDamageAssessmentTasks } from '@/api/myTasks.api';
import { MyTasksPage } from '@/pages/tasks/MyTasksPage';

const activeEvent: UserEventResponse = {
    id: 'evt-1',
    title: 'Kadıköy Bina Çökmesi',
    status: 'OPEN',
    team: { id: 'team-1', name: 'SEARCH_RESCUE', teamCode: 'KAD-1' } as any,
    neighborhood: { id: 'n1', name: 'Moda', districtId: 'd1', districtName: 'Kadıköy' } as any,
    joinedAt: new Date().toISOString(),
    volunteerStatus: 'ASSIGNED',
};

const activeDamageTask: DamageAssessmentTaskResponse = {
    assignmentId: 'assign-1',
    damageAssessmentId: 'dmg-1',
    address: 'Test Adres',
    districtName: 'Kadıköy',
    neighborhoodName: 'Moda',
    damageLevel: 'HEAVY',
    damageLevelLabel: 'Ağır',
    collapseRisk: false,
    emergencyEvacuationNeeded: false,
    casualtiesSuspected: false,
    gasLeakRisk: false,
    assignmentStatus: 'ACTIVE',
    assignmentStatusLabel: 'Aktif',
    assignedByName: 'Koordinatör Kişi',
    assignedAt: new Date().toISOString(),
    active: true,
    reporterPhotoUrls: [],
    fieldPhotoUrls: [],
};

function paged<T>(content: T[]) {
    return { content, totalElements: content.length, totalPages: 1, size: 50, number: 0, last: true, first: true, empty: content.length === 0 };
}

beforeEach(() => {
    vi.clearAllMocks();
    (getMyEvents as any).mockResolvedValue(paged([activeEvent]));
    (getMyDamageAssessmentTasks as any).mockResolvedValue([activeDamageTask]);
});

describe('MyTasksPage (post-refactor)', () => {
    it('renders the active event and active damage-assessment task sections with their content', async () => {
        render(<ToastProvider><MyTasksPage /></ToastProvider>);

        expect(await screen.findByText('Kadıköy Bina Çökmesi')).toBeInTheDocument();
        expect(screen.getByText('Test Adres')).toBeInTheDocument();
        expect(screen.getByText(/Atayan: Koordinatör Kişi/)).toBeInTheDocument();
    });

    it('shows an empty-state message when there are no tasks', async () => {
        (getMyEvents as any).mockResolvedValue(paged([]));
        (getMyDamageAssessmentTasks as any).mockResolvedValue([]);
        render(<ToastProvider><MyTasksPage /></ToastProvider>);

        expect(await screen.findByText('Şu anda aktif bir ekip göreviniz bulunmuyor.')).toBeInTheDocument();
        expect(screen.getByText('Şu anda aktif bir hasar tespiti göreviniz bulunmuyor.')).toBeInTheDocument();
    });
});
