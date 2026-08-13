import React, { ReactElement, useId } from 'react';
import { cn } from '@/utils/cn';

interface FormFieldProps {
    label: string;
    error?: string;
    required?: boolean;
    hint?: string;
    children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>;
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
    const generatedId = useId();
    const inputId = children.props.id ?? generatedId;
    const hintId = hint ? `${inputId}-hint` : undefined;
    const errorId = error ? `${inputId}-error` : undefined;
    const describedBy = [errorId, !error ? hintId : undefined].filter(Boolean).join(' ') || undefined;

    return (
        <div className={cn('flex flex-col space-y-1.5', className)}>
            <label htmlFor={inputId} className="text-sm font-medium text-gray-700">
                {label}
                {required && <span className="ml-1 text-red-500">*</span>}
            </label>

            {React.cloneElement(children, {
                id: inputId,
                'aria-invalid': !!error,
                'aria-describedby': describedBy,
            })}

            {hint && !error && (
                <p id={hintId} className="text-xs text-gray-500">{hint}</p>
            )}

            {error && (
                <p id={errorId} className="text-sm text-red-600 font-medium" role="alert">
                    {error}
                </p>
            )}
        </div>
    );
};
