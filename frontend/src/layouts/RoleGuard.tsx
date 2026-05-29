import React, { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Role } from '@/types';

export const RoleGuard: React.FC<{ allowedRoles: Role[]; children: ReactNode }> = ({ allowedRoles, children }) => {
    const user = useAuthStore((s) => s.user);

    if (!user || (!allowedRoles.includes(user.role) && !allowedRoles.includes('ADMIN'))) {
        // Hide UI components if not allowed
        return null;
    }

    return <>{children}</>;
};

export const ProtectedRoute: React.FC<{ allowedRoles?: Role[]; children: ReactNode }> = ({ allowedRoles, children }) => {
    const user = useAuthStore((s) => s.user);

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (allowedRoles && !allowedRoles.includes(user.role)) {
        return <Navigate to="/unauthorized" replace />;
    }

    return <>{children}</>;
};
