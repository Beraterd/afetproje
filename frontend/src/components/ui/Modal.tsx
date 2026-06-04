import React, { ReactNode, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '@/utils/cn';

export interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    size?: 'sm' | 'md' | 'lg';
    footer?: ReactNode;
    children: ReactNode;
    className?: string;
}

export const Modal: React.FC<ModalProps> = ({
    isOpen,
    onClose,
    title,
    size = 'md',
    footer,
    children,
    className,
}) => {
    useEffect(() => {
        const handleEscape = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
            document.body.style.overflow = 'hidden';
        }
        return () => {
            document.removeEventListener('keydown', handleEscape);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    const sizeClasses = {
        sm: 'max-w-md',
        md: 'max-w-lg',
        lg: 'max-w-3xl',
    };

    return createPortal(
        <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto overflow-x-hidden bg-black bg-opacity-50 p-4">
            <div className="fixed inset-0" onClick={onClose} aria-hidden="true" />
            <div
                className={cn(
                    'relative w-full rounded-lg bg-white shadow-xl',
                    sizeClasses[size],
                    className
                )}
                role="dialog"
                aria-modal="true"
                aria-labelledby="modal-title"
            >
                <div className="flex items-center justify-between border-b pb-4 pt-5 px-6">
                    <h3 className="text-lg font-medium leading-6 text-gray-900" id="modal-title">
                        {title}
                    </h3>
                    <button
                        type="button"
                        className="rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                        onClick={onClose}
                    >
                        <span className="sr-only">Kapat</span>
                        <X className="h-6 w-6" aria-hidden="true" />
                    </button>
                </div>
                <div className="px-6 py-5">{children}</div>
                {footer && <div className="border-t bg-gray-50 px-6 py-4 rounded-b-lg">{footer}</div>}
            </div>
        </div>,
        document.body
    );
};
