import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export const AuthLayout: React.FC = () => {
    const { accessToken } = useAuthStore();

    // If user is already logged in, redirect away from auth pages to dashboard
    if (accessToken) {
        return <Navigate to="/dashboard" replace />;
    }

    return (
        <div className="flex min-h-screen bg-gray-50 flex-col justify-center py-12 sm:px-6 lg:px-8">
            <Outlet />
        </div>
    );
};
