import React, { ReactElement } from 'react';
import { cn } from '@/utils/cn';

interface FormFieldProps {
    label: string;
    error?: string;
    required?: boolean;
    hint?: string;
    children: ReactElement;
    className?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
    label,
    error,
    required,
    hint,
    children,
    className,
}) => {
    return (
        <div className={cn('flex flex-col space-y-1.5', className)}>
            <label className="text-sm font-medium text-gray-700">
                {label}
                {required && <span className="ml-1 text-red-500">*</span>}
            </label>

            {children}

            {hint && !error && (
                <p className="text-xs text-gray-500">{hint}</p>
            )}

            {error && (
                <p className="text-sm text-red-600 font-medium" role="alert">
                    {error}
                </p>
            )}
        </div>
    );
};
