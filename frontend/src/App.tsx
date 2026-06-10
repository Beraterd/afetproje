import { Navigate, Outlet, useRoutes } from 'react-router-dom';

import { AuthLayout } from '@/layouts/AuthLayout';
import { AppLayout } from '@/layouts/AppLayout';
import { ProtectedRoute } from '@/layouts/RoleGuard';
import { ToastProvider } from '@/components/shared/ToastProvider';
import { AuthProvider } from '@/components/shared/AuthProvider';

import { LoginPage } from '@/pages/auth/LoginPage';
import { RegisterPage } from '@/pages/auth/RegisterPage';
import { VerifyEmailPage } from '@/pages/auth/VerifyEmailPage';
import { ProfilePage } from '@/pages/auth/ProfilePage';
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage';

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
import { MaintenancePage } from '@/pages/admin/MaintenancePage';
import { DistrictsPage } from '@/pages/admin/DistrictsPage';
import { CoordinatorAssignmentPage } from '@/pages/admin/CoordinatorAssignmentPage';
import { AssemblyAreaReviewPage } from '@/pages/admin/AssemblyAreaReviewPage';
import { AuditPage } from '@/pages/admin/AuditPage';

import { MyTasksPage } from '@/pages/tasks/MyTasksPage';
import { EmergencyContactsPage } from '@/pages/profile/EmergencyContactsPage';
import { EmergencyAssemblyAreasPage } from '@/pages/emergency/EmergencyAssemblyAreasPage';
import { MyRecordsPage } from '@/pages/profile/MyRecordsPage';
import { DamageAssessmentsPage } from '@/pages/damage/DamageAssessmentsPage';
import { ResourceRequestsPage } from '@/pages/resources/ResourceRequestsPage';
import { CoordinationCenterPage } from '@/pages/coordination/CoordinationCenterPage';
import { EarthquakesPage } from '@/pages/earthquakes/EarthquakesPage';
import { ReportCenterPage } from '@/pages/reports/ReportCenterPage';
import { ReportViewPage } from '@/pages/reports/ReportViewPage';

import { UnauthorizedPage } from '@/pages/errors/UnauthorizedPage';
import { NotFoundPage } from '@/pages/errors/NotFoundPage';
import { AssignmentAcceptPage } from '@/pages/assignment/AssignmentAcceptPage';
import { AssignmentDeclinePage } from '@/pages/assignment/AssignmentDeclinePage';
import { EmergencyMessageResultPage } from '@/pages/emergency/EmergencyMessageResultPage';
import { EmergencyStatusPage } from '@/pages/emergency/EmergencyStatusPage';

function AppRoutes() {
    return useRoutes([
        { path: '/', element: <Navigate to="/dashboard" replace /> },

        {
            element: <AuthLayout />,
            children: [
                { path: 'login', element: <LoginPage /> },
                { path: 'register', element: <RegisterPage /> },
                { path: 'verify-email', element: <VerifyEmailPage /> },
                { path: 'forgot-password', element: <ForgotPasswordPage /> },
                { path: 'reset-password', element: <ResetPasswordPage /> },
            ],
        },

        {
            element: <AppLayout />,
            children: [
                { path: 'profile', element: <ProfilePage /> },
                { path: 'my-tasks', element: <MyTasksPage /> },
                { path: 'emergency-contacts', element: <EmergencyContactsPage /> },
                { path: 'emergency/assembly-areas', element: <EmergencyAssemblyAreasPage /> },
                { path: 'my-records', element: <MyRecordsPage /> },
                { path: 'damage-assessments', element: <DamageAssessmentsPage /> },
                { path: 'resource-requests', element: <ResourceRequestsPage /> },
                { path: 'dashboard', element: <DashboardPage /> },
                {
                    path: 'coordination-center',
                    element: (
                        <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}>
                            <CoordinationCenterPage />
                        </ProtectedRoute>
                    ),
                },

                {
                    path: 'reports',
                    element: (
                        <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}>
                            <Outlet />
                        </ProtectedRoute>
                    ),
                    children: [
                        { path: '', element: <ReportCenterPage /> },
                        { path: 'view', element: <ReportViewPage /> },
                    ],
                },

                {
                    path: 'events',
                    element: <Outlet />,
                    children: [
                        { path: '', element: <EventsPage /> },
                        {
                            path: 'create',
                            element: (
                                <ProtectedRoute
                                    allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}
                                >
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
                                <ProtectedRoute
                                    allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR']}
                                >
                                    <DocumentApprovalPage />
                                </ProtectedRoute>
                            ),
                        },
                    ],
                },

                { path: 'map', element: <MapPage /> },
                { path: 'earthquakes', element: <EarthquakesPage /> },

                {
                    path: 'simulations',
                    element: (
                        <ProtectedRoute allowedRoles={['ADMIN']}>
                            <Outlet />
                        </ProtectedRoute>
                    ),
                    children: [
                        { path: '', element: <SimulationTriggerPage /> },
                        { path: ':id', element: <SimulationDetailPage /> },
                    ],
                },

                {
                    path: 'admin',
                    element: (
                        <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR']}>
                            <Outlet />
                        </ProtectedRoute>
                    ),
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
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <UsersPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'districts',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <DistrictsPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'assembly-areas',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <AssemblyAreaReviewPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'maintenance',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <MaintenancePage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'audit',
                            element: (
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <AuditPage />
                                </ProtectedRoute>
                            ),
                        },
                    ],
                },

                { path: 'unauthorized', element: <UnauthorizedPage /> },
            ],
        },

        { path: 'assignment/accept', element: <AssignmentAcceptPage /> },
        { path: 'assignment/decline', element: <AssignmentDeclinePage /> },
        { path: 'emergency-message-result', element: <EmergencyMessageResultPage /> },
        { path: 'emergency-status/:token', element: <EmergencyStatusPage /> },

        { path: '*', element: <NotFoundPage /> },
    ]);
}

export default function App() {
    return (
        <AuthProvider>
            <ToastProvider>
                <AppRoutes />
            </ToastProvider>
        </AuthProvider>
    );
}