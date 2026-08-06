import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, CheckCircle, AlertTriangle, AlertCircle, Info } from 'lucide-react';
import { cn } from '@/utils/cn';
import { DEMO_MODE_BLOCKED_EVENT } from '@/api/axiosInstance';

type ToastType = 'success' | 'error' | 'warning' | 'info';

interface ToastOptions {
    id: string;
    type: ToastType;
    message: string;
    createdAt: number;
}

interface ToastContextValue {
    success: (msg: string) => void;
    error: (msg: string) => void;
    warning: (msg: string) => void;
    info: (msg: string) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export const useToast = () => {
    const context = useContext(ToastContext);
    if (!context) throw new Error('useToast must be used within a ToastProvider');
    return context;
};

const DURATION = 5000;

const TITLE: Record<ToastType, string> = {
    success: 'Başarılı',
    error: 'Hata',
    warning: 'Uyarı',
    info: 'Bilgi',
};

const ICONS: Record<ToastType, React.ReactNode> = {
    success: <CheckCircle className="h-6 w-6 text-green-500 flex-shrink-0" />,
    error: <AlertCircle className="h-6 w-6 text-red-500 flex-shrink-0" />,
    warning: <AlertTriangle className="h-6 w-6 text-yellow-500 flex-shrink-0" />,
    info: <Info className="h-6 w-6 text-blue-500 flex-shrink-0" />,
};

const STYLES: Record<ToastType, { container: string; bar: string; title: string }> = {
    success: {
        container: 'border-l-4 border-green-500 bg-white',
        bar: 'bg-green-500',
        title: 'text-green-700',
    },
    error: {
        container: 'border-l-4 border-red-500 bg-white',
        bar: 'bg-red-500',
        title: 'text-red-700',
    },
    warning: {
        container: 'border-l-4 border-yellow-500 bg-white',
        bar: 'bg-yellow-500',
        title: 'text-yellow-700',
    },
    info: {
        container: 'border-l-4 border-blue-500 bg-white',
        bar: 'bg-blue-500',
        title: 'text-blue-700',
    },
};

const ToastItem: React.FC<{ toast: ToastOptions; onRemove: (id: string) => void }> = ({ toast, onRemove }) => {
    const [width, setWidth] = useState(100);
    const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const style = STYLES[toast.type];

    useEffect(() => {
        const step = 100 / (DURATION / 50);
        intervalRef.current = setInterval(() => {
            setWidth((w) => {
                if (w <= 0) {
                    if (intervalRef.current) clearInterval(intervalRef.current);
                    return 0;
                }
                return w - step;
            });
        }, 50);
        return () => {
            if (intervalRef.current) clearInterval(intervalRef.current);
        };
    }, []);

    return (
        <div
            className={cn(
                'pointer-events-auto w-full rounded-lg shadow-2xl overflow-hidden',
                style.container
            )}
            role="alert"
            style={{ animation: 'slideIn 0.25s ease-out' }}
        >
            <div className="flex items-start gap-3 px-4 py-4">
                {ICONS[toast.type]}
                <div className="flex-1 min-w-0">
                    <p className={cn('text-sm font-bold leading-5', style.title)}>
                        {TITLE[toast.type]}
                    </p>
                    <p className="mt-0.5 text-sm text-gray-700 leading-5 break-words">
                        {toast.message}
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => onRemove(toast.id)}
                    className="flex-shrink-0 ml-1 rounded-md p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors focus:outline-none"
                    aria-label="Kapat"
                >
                    <X className="h-4 w-4" />
                </button>
            </div>
            {/* Progress bar */}
            <div className="h-1 bg-gray-100">
                <div
                    className={cn('h-full transition-none', style.bar)}
                    style={{ width: `${width}%` }}
                />
            </div>
        </div>
    );
};

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [toasts, setToasts] = useState<ToastOptions[]>([]);

    const addToast = useCallback((type: ToastType, message: string) => {
        const id = Math.random().toString(36).substring(2, 9);
        setToasts((prev) => {
            const newToasts = [...prev, { id, type, message, createdAt: Date.now() }];
            return newToasts.length > 5 ? newToasts.slice(-5) : newToasts;
        });
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id));
        }, DURATION + 300);
    }, []);

    const removeToast = useCallback((id: string) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    // Demo modda backend'in reddettiği yazma istekleri, sayfa/aksiyon fark etmeksizin
    // buradan tek bir uyarı toast'ı olarak gösterilir (bkz. axiosInstance.ts).
    useEffect(() => {
        const handler = (e: Event) => {
            const message = (e as CustomEvent<string>).detail || 'Bu işlem demo modunda kullanılamaz.';
            addToast('warning', message);
        };
        window.addEventListener(DEMO_MODE_BLOCKED_EVENT, handler);
        return () => window.removeEventListener(DEMO_MODE_BLOCKED_EVENT, handler);
    }, [addToast]);

    const val: ToastContextValue = {
        success: (m) => addToast('success', m),
        error: (m) => addToast('error', m),
        warning: (m) => addToast('warning', m),
        info: (m) => addToast('info', m),
    };

    return (
        <ToastContext.Provider value={val}>
            {children}
            {createPortal(
                <>
                    <style>{`
                        @keyframes slideIn {
                            from { opacity: 0; transform: translateX(100%); }
                            to   { opacity: 1; transform: translateX(0); }
                        }
                    `}</style>
                    <div
                        className="fixed top-20 right-4 z-[9999] flex flex-col items-end gap-3 pointer-events-none"
                        style={{ maxWidth: '420px', width: 'calc(100vw - 2rem)' }}
                    >
                        {toasts.map((toast) => (
                            <ToastItem key={toast.id} toast={toast} onRemove={removeToast} />
                        ))}
                    </div>
                </>,
                document.body
            )}
        </ToastContext.Provider>
    );
};
