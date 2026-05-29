import React from 'react';
import { cn } from '@/utils/cn';

interface LoadingSpinnerProps {
    size?: 'sm' | 'md' | 'lg';
    label?: string;
    className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 'md', label = 'Loading...', className }) => {
    const sizeClasses = {
        sm: 'w-4 h-4 border-2',
        md: 'w-8 h-8 border-4',
        lg: 'w-12 h-12 border-4',
    };

    return (
        <div className={cn('flex flex-col items-center justify-center space-y-2', className)} role="status">
            <div
                className={cn(
                    'animate-spin rounded-full border-blue-200 border-t-blue-600',
                    sizeClasses[size]
                )}
            />
            {label && <span className="sr-only">{label}</span>}
        </div>
    );
};
