import React, { ReactNode } from 'react';
import { cn } from '@/utils/cn';

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'neutral' | 'info' | 'high';

interface BadgeProps {
    variant?: BadgeVariant;
    children: ReactNode;
    className?: string;
}

export const Badge: React.FC<BadgeProps> = ({ variant = 'neutral', children, className }) => {
    const variantClasses: Record<BadgeVariant, string> = {
        success: 'bg-brand-100 text-brand-800 border-brand-200',
        warning: 'bg-yellow-50 text-yellow-800 border-yellow-200',
        danger: 'bg-red-50 text-red-800 border-red-200',
        neutral: 'bg-gray-50 text-gray-800 border-gray-200',
        info: 'bg-blue-50 text-blue-800 border-blue-200',
        high: 'bg-orange-50 text-orange-800 border-orange-200',
    };

    return (
        <span
            className={cn(
                'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border shadow-sm',
                variantClasses[variant],
                className
            )}
        >
            {children}
        </span>
    );
};
