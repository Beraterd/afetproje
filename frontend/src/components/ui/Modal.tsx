import React, { ReactNode, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '@/utils/cn';

const FOCUSABLE_SELECTOR =
    'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

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
    const dialogRef = useRef<HTMLDivElement>(null);
    const triggerElementRef = useRef<Element | null>(null);

    useEffect(() => {
        const handleKeydown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                onClose();
                return;
            }
            if (e.key === 'Tab' && dialogRef.current) {
                const focusable = dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
                if (focusable.length === 0) return;
                const first = focusable[0];
                const last = focusable[focusable.length - 1];
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        };

        if (isOpen) {
            triggerElementRef.current = document.activeElement;
            document.addEventListener('keydown', handleKeydown);
            document.body.style.overflow = 'hidden';
            dialogRef.current?.focus();
        }
        return () => {
            document.removeEventListener('keydown', handleKeydown);
            document.body.style.overflow = 'unset';
            if (isOpen) {
                (triggerElementRef.current as HTMLElement | null)?.focus?.();
            }
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
                ref={dialogRef}
                tabIndex={-1}
                className={cn(
                    'relative w-full rounded-lg bg-white shadow-xl focus:outline-none',
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
                        aria-label="Kapat"
                        className="rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
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
