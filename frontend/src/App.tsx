import { lazy, Suspense } from 'react';
import { Navigate, Outlet, useRoutes } from 'react-router-dom';

import { AuthLayout } from '@/layouts/AuthLayout';
import { AppLayout } from '@/layouts/AppLayout';
import { ProtectedRoute } from '@/layouts/RoleGuard';
import { ToastProvider } from '@/components/shared/ToastProvider';
import { AuthProvider } from '@/components/shared/AuthProvider';
import { LoadingSpinner } from '@/components/ui';

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

import { MyTasksPage } from '@/pages/tasks/MyTasksPage';
import { EmergencyContactsPage } from '@/pages/profile/EmergencyContactsPage';
import { EmergencyAssemblyAreasPage } from '@/pages/emergency/EmergencyAssemblyAreasPage';
import { MyRecordsPage } from '@/pages/profile/MyRecordsPage';
import { DamageAssessmentsPage } from '@/pages/damage/DamageAssessmentsPage';
import { ResourceRequestsPage } from '@/pages/resources/ResourceRequestsPage';
import { EarthquakesPage } from '@/pages/earthquakes/EarthquakesPage';

import { UnauthorizedPage } from '@/pages/errors/UnauthorizedPage';
import { NotFoundPage } from '@/pages/errors/NotFoundPage';
import { AssignmentAcceptPage } from '@/pages/assignment/AssignmentAcceptPage';
import { AssignmentDeclinePage } from '@/pages/assignment/AssignmentDeclinePage';
import { EmergencyMessageResultPage } from '@/pages/emergency/EmergencyMessageResultPage';
import { EmergencyStatusPage } from '@/pages/emergency/EmergencyStatusPage';

// Ağır sayfalar (Leaflet harita, rapor grafikleri, admin/simülasyon ekranları) route-level
// lazy load edilir — ilk yüklemede bundle'a dahil edilmezler, yalnızca route ziyaret
// edildiğinde indirilirler. Küçük component'ler bilerek lazy yapılmadı.
const MapPage = lazy(() => import('@/pages/map/MapPage').then((m) => ({ default: m.MapPage })));
const Map3DPage = lazy(() => import('@/pages/map/Map3DPage').then((m) => ({ default: m.Map3DPage })));
const SimulationTriggerPage = lazy(() =>
    import('@/pages/simulations/SimulationTriggerPage').then((m) => ({ default: m.SimulationTriggerPage })));
const SimulationDetailPage = lazy(() =>
    import('@/pages/simulations/SimulationDetailPage').then((m) => ({ default: m.SimulationDetailPage })));
const UsersPage = lazy(() => import('@/pages/admin/UsersPage').then((m) => ({ default: m.UsersPage })));
const MaintenancePage = lazy(() => import('@/pages/admin/MaintenancePage').then((m) => ({ default: m.MaintenancePage })));
const DistrictsPage = lazy(() => import('@/pages/admin/DistrictsPage').then((m) => ({ default: m.DistrictsPage })));
const CoordinatorAssignmentPage = lazy(() =>
    import('@/pages/admin/CoordinatorAssignmentPage').then((m) => ({ default: m.CoordinatorAssignmentPage })));
const AssemblyAreaReviewPage = lazy(() =>
    import('@/pages/admin/AssemblyAreaReviewPage').then((m) => ({ default: m.AssemblyAreaReviewPage })));
const AuditPage = lazy(() => import('@/pages/admin/AuditPage').then((m) => ({ default: m.AuditPage })));
const CoordinationCenterPage = lazy(() =>
    import('@/pages/coordination/CoordinationCenterPage').then((m) => ({ default: m.CoordinationCenterPage })));
const ReportCenterPage = lazy(() => import('@/pages/reports/ReportCenterPage').then((m) => ({ default: m.ReportCenterPage })));
const ReportViewPage = lazy(() => import('@/pages/reports/ReportViewPage').then((m) => ({ default: m.ReportViewPage })));

const PageLoadingFallback = () => (
    <div className="flex items-center justify-center py-24">
        <LoadingSpinner size="lg" label="Sayfa yükleniyor..." />
    </div>
);

/** Lazy route element'lerini Suspense ile sarar — her route için ayrı ayrı yazmamak için. */
const withSuspense = (element: JSX.Element) => <Suspense fallback={<PageLoadingFallback />}>{element}</Suspense>;

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
                    element: withSuspense(
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
                        { path: '', element: withSuspense(<ReportCenterPage />) },
                        { path: 'view', element: withSuspense(<ReportViewPage />) },
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

                { path: 'map', element: withSuspense(<MapPage />) },
                { path: 'map-3d', element: withSuspense(<Map3DPage />) },
                { path: 'earthquakes', element: <EarthquakesPage /> },

                {
                    path: 'simulations',
                    element: (
                        <ProtectedRoute allowedRoles={['ADMIN']}>
                            <Outlet />
                        </ProtectedRoute>
                    ),
                    children: [
                        { path: '', element: withSuspense(<SimulationTriggerPage />) },
                        { path: ':id', element: withSuspense(<SimulationDetailPage />) },
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
                            element: withSuspense(
                                <ProtectedRoute allowedRoles={['ADMIN', 'DISTRICT_COORDINATOR']}>
                                    <CoordinatorAssignmentPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'users',
                            element: withSuspense(
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <UsersPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'districts',
                            element: withSuspense(
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <DistrictsPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'assembly-areas',
                            element: withSuspense(
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <AssemblyAreaReviewPage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'maintenance',
                            element: withSuspense(
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <MaintenancePage />
                                </ProtectedRoute>
                            ),
                        },
                        {
                            path: 'audit',
                            element: withSuspense(
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