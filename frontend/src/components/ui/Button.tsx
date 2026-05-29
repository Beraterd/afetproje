import React, { ReactNode } from 'react';
import { cn } from '@/utils/cn';
import { LoadingSpinner } from './LoadingSpinner';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
    size?: 'sm' | 'md' | 'lg';
    loading?: boolean;
    leftIcon?: ReactNode;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
    ({ className, variant = 'primary', size = 'md', loading = false, disabled, leftIcon, children, ...props }, ref) => {

        const baseClasses = 'inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none active:scale-95';

        const variantClasses = {
            primary: 'bg-brand-600 text-white hover:bg-brand-700 focus:ring-brand-500 shadow-sm hover:shadow-md hover:-translate-y-0.5',
            secondary: 'bg-white text-brand-700 border border-brand-200 hover:bg-brand-50 hover:border-brand-300 focus:ring-brand-500 shadow-sm hover:shadow',
            danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 shadow-sm hover:shadow-md hover:-translate-y-0.5',
            ghost: 'bg-transparent text-gray-600 hover:bg-brand-50 hover:text-brand-700 focus:ring-brand-500',
        };

        const sizeClasses = {
            sm: 'h-8 px-3 text-sm',
            md: 'h-10 px-4 text-sm',
            lg: 'h-12 px-6 text-base',
        };

        return (
            <button
                ref={ref}
                className={cn(baseClasses, variantClasses[variant], sizeClasses[size], className)}
                disabled={disabled || loading}
                {...props}
            >
                {loading ? (
                    <LoadingSpinner size="sm" className="mr-2 border-current border-t-transparent" label="" />
                ) : leftIcon ? (
                    <span className="mr-2">{leftIcon}</span>
                ) : null}
                {children}
            </button>
        );
    }
);

Button.displayName = 'Button';
