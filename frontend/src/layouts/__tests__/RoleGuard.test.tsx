import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute, RoleGuard } from '@/layouts/RoleGuard';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

function renderGuard(allowedRoles: UserSummaryResponse['role'][]) {
    return render(
        <RoleGuard allowedRoles={allowedRoles}>
            <div>Gizli İçerik</div>
        </RoleGuard>,
    );
}

function renderProtected(allowedRoles?: UserSummaryResponse['role'][]) {
    return render(
        <MemoryRouter initialEntries={['/secret']}>
            <Routes>
                <Route
                    path="/secret"
                    element={
                        <ProtectedRoute allowedRoles={allowedRoles}>
                            <div>Gizli İçerik</div>
                        </ProtectedRoute>
                    }
                />
                <Route path="/login" element={<div>Giriş Sayfası</div>} />
                <Route path="/unauthorized" element={<div>Yetkisiz Sayfası</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null });
});

describe('ProtectedRoute', () => {
    it('redirects an anonymous user to /login', () => {
        renderProtected(['ADMIN']);
        expect(screen.getByText('Giriş Sayfası')).toBeInTheDocument();
        expect(screen.queryByText('Gizli İçerik')).not.toBeInTheDocument();
    });

    it('redirects a logged-in user with a disallowed role to /unauthorized', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'VOLUNTEER' },
        });
        renderProtected(['ADMIN']);
        expect(screen.getByText('Yetkisiz Sayfası')).toBeInTheDocument();
        expect(screen.queryByText('Gizli İçerik')).not.toBeInTheDocument();
    });

    it('renders children for a user with an allowed role', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'ADMIN' },
        });
        renderProtected(['ADMIN']);
        expect(screen.getByText('Gizli İçerik')).toBeInTheDocument();
    });

    it('renders children for any authenticated user when no allowedRoles is given', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'VOLUNTEER' },
        });
        renderProtected(undefined);
        expect(screen.getByText('Gizli İçerik')).toBeInTheDocument();
    });
});

describe('RoleGuard', () => {
    it('hides content for a VOLUNTEER when only ADMIN is allowed', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'VOLUNTEER' },
        });
        renderGuard(['ADMIN']);
        expect(screen.queryByText('Gizli İçerik')).not.toBeInTheDocument();
    });

    it('renders content for an ADMIN when ADMIN is allowed', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'ADMIN' },
        });
        renderGuard(['ADMIN']);
        expect(screen.getByText('Gizli İçerik')).toBeInTheDocument();
    });

    it('hides content for a VOLUNTEER when allowed roles are coordinator/admin only', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'VOLUNTEER' },
        });
        renderGuard(['DISTRICT_COORDINATOR', 'ADMIN']);
        expect(screen.queryByText('Gizli İçerik')).not.toBeInTheDocument();
    });

    it('renders content for a DISTRICT_COORDINATOR when allowed roles are coordinator/admin', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'DISTRICT_COORDINATOR' },
        });
        renderGuard(['DISTRICT_COORDINATOR', 'ADMIN']);
        expect(screen.getByText('Gizli İçerik')).toBeInTheDocument();
    });

    it('renders content for a NEIGHBORHOOD_COORDINATOR when only that role is allowed', () => {
        useAuthStore.setState({
            accessToken: 't',
            user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com', role: 'NEIGHBORHOOD_COORDINATOR' },
        });
        renderGuard(['NEIGHBORHOOD_COORDINATOR']);
        expect(screen.getByText('Gizli İçerik')).toBeInTheDocument();
    });

    it('hides content for an anonymous (unauthenticated) user', () => {
        useAuthStore.setState({ accessToken: null, user: null });
        renderGuard(['ADMIN']);
        expect(screen.queryByText('Gizli İçerik')).not.toBeInTheDocument();
    });
});
