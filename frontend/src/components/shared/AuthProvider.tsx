import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import { getMe } from '@/api/users.api';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isInitializing, setIsInitializing] = useState(true);
    const { accessToken, setAuth, clearAuth } = useAuthStore();

    useEffect(() => {
        const initializeAuth = async () => {
            // If we already have a token in memory, no need to initialize
            if (accessToken) {
                setIsInitializing(false);
                return;
            }

            const storedToken = localStorage.getItem('afet_token');
            if (!storedToken) {
                clearAuth();
                setIsInitializing(false);
                return;
            }

            try {
                // Temporarily set the token so the axios interceptor sends the Authorization header
                useAuthStore.setState({ accessToken: storedToken });

                // Verify token and fetch user profile
                const me = await getMe();

                setAuth(storedToken, {
                    id: me.id,
                    firstName: me.firstName,
                    lastName: me.lastName,
                    email: me.email,
                    role: me.role,
                    districtId: me.districtId,
                    neighborhoodId: me.neighborhoodId,
                    locationPermissionStatus: me.locationPermissionStatus,
                    demo: me.demo,
                });
            } catch (error) {
                console.error('Failed to recover session:', error);
                clearAuth();
                localStorage.removeItem('afet_token');
            } finally {
                setIsInitializing(false);
            }
        };

        initializeAuth();
    }, []);

    if (isInitializing) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-gray-50">
                <LoadingSpinner size="lg" label="Initializing application..." />
            </div>
        );
    }

    return <>{children}</>;
};
