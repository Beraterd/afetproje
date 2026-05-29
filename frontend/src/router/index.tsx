import { Outlet, useRoutes, Navigate } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { AppLayout } from '@/layouts/AppLayout';
import { ProtectedRoute } from '@/layouts/RoleGuard';

import { LoginPage } from '@/pages/auth/LoginPage';
import { RegisterPage } from '@/pages/auth/RegisterPage';
import { VerifyEmailPage } from '@/pages/auth/VerifyEmailPage';
import { ProfilePage } from '@/pages/auth/ProfilePage';

import { DashboardPage } from '@/pages/dashboard/DashboardPage';

import { EventsPage } from '@/pages/events/EventsPage';
import { EventDetailPage } from '@/pages/events/EventDetailPage';
import { CreateEventPage } from '@/pages/events/CreateEventPage';

import { DocumentsPage } from '@/pages/documents/DocumentsPage';
import { DocumentApprovalPage } from '@/pages/documents/DocumentApprovalPage';

import { MapPage } from '@/pages/map/MapPage';

import { SimulationTriggerPage } from '@/pages/simulations/SimulationTriggerPage';
import { SimulationDetailPage } from '@/pages/simulations/SimulationDetailPage';

import { UsersPage } from '@/pages/admin/UsersPage';
import { CoordinatorAssignmentPage } from '@/pages/admin/CoordinatorAssignmentPage';
import { AuditPage } from '@/pages/admin/AuditPage';

import { MyTasksPage } from '@/pages/tasks/MyTasksPage';
import { DamageAssessmentsPage } from '@/pages/damage/DamageAssessmentsPage';
import { EmergencyContactsPage } from '@/pages/profile/EmergencyContactsPage';

import { UnauthorizedPage } from '@/pages/errors/UnauthorizedPage';
import { NotFoundPage } from '@/pages/errors/NotFoundPage';

function AppRoutes() {
    const element = useRoutes([
        {
            path: '/',
            element: <Navigate to="/dashboard" replace />,
        },
        {
            element: <AuthLayout />,
            children: [
                { path: 'login', element: <LoginPage /> },
                { path: 'register', element: <RegisterPage /> },
                { path: 'verify-email', element: <VerifyEmailPage /> },
            ],
        },
        {
            element: <AppLayout />,
            children: [
                {
                    path: 'profile',
                    element: <ProfilePage />,
                },
                {
                    path: 'my-tasks',
                    element: <MyTasksPage />,
                },
                {
                    path: 'emergency-contacts',
                    element: <EmergencyContactsPage />,
                },
                {
                    path: 'dashboard',
                    element: <DashboardPage />,
                },
                {
                    path: 'events',
                    element: <Outlet />,
                    children: [
                        { path: '', element: <EventsPage /> },
                        {
                            path: 'create',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}>
                                    <CreateEventPage />
                                </ProtectedRoute>
                            ),
                        },
                        { path: ':id', element: <EventDetailPage /> },
                    ],
                },
                {
                    path: 'documents',
                    element: <Outlet />,
                    children: [
                        { path: '', element: <DocumentsPage /> },
                        {
                            path: 'approvals',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}>
                                    <DocumentApprovalPage />
                                </ProtectedRoute>
                            ),
                        },
                    ],
                },
                {
                    path: 'map',
                    element: <MapPage />,
                },
                {
                    path: 'damage-assessments',
                    element: <DamageAssessmentsPage />,
                },
                {
                    path: 'simulations',
                    element: <ProtectedRoute allowedRoles={['ADMIN']}><Outlet /></ProtectedRoute>,
                    children: [
                        { path: '', element: <SimulationTriggerPage /> },
                        { path: ':id', element: <SimulationDetailPage /> },
                    ],
                },
                {
                    path: 'admin',
                    element: <Outlet />,
                    children: [
                        {
                            path: 'coordinators',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR']}>
                                    <CoordinatorAssignmentPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'users',
                            element: <ProtectedRoute allowedRoles={['ADMIN']}><UsersPage /></ProtectedRoute>,
                        },
                        {
                            path: 'audit',
                            element: <ProtectedRoute allowedRoles={['ADMIN']}><AuditPage /></ProtectedRoute>,
                        },
                    ],
                },
                { path: 'unauthorized', element: <UnauthorizedPage /> },
            ],
        },
        { path: '*', element: <NotFoundPage /> },
    ]);

    return element;
}

export default AppRoutes;
