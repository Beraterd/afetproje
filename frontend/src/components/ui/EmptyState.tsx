import React, { ReactNode } from 'react';
import { cn } from '@/utils/cn';
import { Button } from './Button';

interface EmptyStateProps {
    icon?: ReactNode;
    title: string;
    description?: string;
    action?: { label: string; onClick: () => void };
    className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
    icon,
    title,
    description,
    action,
    className,
}) => {
    return (
        <div
            className={cn(
                'flex flex-col items-center justify-center p-8 text-center rounded-lg border-2 border-dashed border-gray-200 bg-gray-50',
                className
            )}
        >
            {icon && <div className="mb-4 text-gray-400">{icon}</div>}
            <h3 className="text-sm font-medium text-gray-900">{title}</h3>
            {description && <p className="mt-1 text-sm text-gray-500">{description}</p>}
            {action && (
                <div className="mt-6">
                    <Button variant="secondary" onClick={action.onClick} size="sm">
                        {action.label}
                    </Button>
                </div>
            )}
        </div>
    );
};
