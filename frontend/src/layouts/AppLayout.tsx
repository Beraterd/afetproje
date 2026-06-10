import React, { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from '@tanstack/react-query';
import { SideNav, TopBar } from './AppNavigation';
import { CoordinationCenterSetupModal } from '@/components/shared/CoordinationCenterSetupModal';
import { LocationPermissionModal } from '@/components/shared/LocationPermissionModal';
import { OfflineStatusBanner } from '@/components/shared/OfflineStatusBanner';
import { getMyCoordinationStatus } from '@/api/coordinationCenters.api';
import { queryKeys } from '@/utils/queryKeys';

export const AppLayout: React.FC = () => {
    const location = useLocation();
    const { accessToken, user } = useAuthStore();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [modalDismissed, setModalDismissed] = useState(false);
    const [locationModalDismissed, setLocationModalDismissed] = useState(false);

    // Close mobile menu when route changes (mirrors memoria)
    useEffect(() => {
        if (mobileMenuOpen) setMobileMenuOpen(false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.pathname]);

    // Lock body scroll when mobile menu is open (mirrors memoria)
    useEffect(() => {
        if (mobileMenuOpen) {
            document.body.style.overflow = 'hidden';
            // Prevent overscroll on iOS
            document.body.style.overscrollBehavior = 'none';
        } else {
            document.body.style.overflow = 'unset';
            document.body.style.overscrollBehavior = 'unset';
        }

        return () => {
            document.body.style.overflow = 'unset';
            document.body.style.overscrollBehavior = 'unset';
        };
    }, [mobileMenuOpen]);

    const isCoordinator =
        user?.role === 'DISTRICT_COORDINATOR' || user?.role === 'NEIGHBORHOOD_COORDINATOR';

    const { data: centerStatus } = useQuery({
        queryKey: queryKeys.coordinationCenters.myStatus(),
        queryFn: getMyCoordinationStatus,
        enabled: !!accessToken && isCoordinator,
        staleTime: 5 * 60 * 1000,
    });

    if (!accessToken) {
        return <Navigate to="/login" replace />;
    }

    const showSetupModal =
        !modalDismissed &&
        isCoordinator &&
        !!centerStatus?.mustSetCenter;

    const showLocationModal =
        !locationModalDismissed &&
        !!accessToken &&
        user?.locationPermissionStatus == null;

    return (
        <div>
            <SideNav mobileOpen={mobileMenuOpen} setMobileOpen={setMobileMenuOpen} />

            <div className="lg:pl-72 flex flex-col min-h-screen">
                <div className={mobileMenuOpen ? 'hidden lg:block' : undefined}>
                    <TopBar onMenuClick={() => setMobileMenuOpen(true)} />
                    <OfflineStatusBanner />
                </div>

                <main className="flex-1 py-10 bg-transparent">
                    <div className="px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
                        <Outlet />
                    </div>
                </main>
            </div>

            {showSetupModal && (
                <CoordinationCenterSetupModal
                    isOpen={showSetupModal}
                    role={user!.role as 'DISTRICT_COORDINATOR' | 'NEIGHBORHOOD_COORDINATOR'}
                    districtName={centerStatus?.assignedDistrictName}
                    neighborhoodName={centerStatus?.assignedNeighborhoodName}
                    onDismiss={() => setModalDismissed(true)}
                />
            )}

            {showLocationModal && (
                <LocationPermissionModal
                    isOpen={showLocationModal}
                    onClose={() => setLocationModalDismissed(true)}
                />
            )}
        </div>
    );
};
