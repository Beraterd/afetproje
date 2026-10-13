import React from 'react';
import { Eye, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export const DemoModeBanner: React.FC = () => {
    const isDemo = useAuthStore((s) => s.user?.demo);

    if (!isDemo) return null;

    return (
        <div
            role="status"
            className="bg-amber-50 border-b border-amber-200 px-4 py-2 flex flex-wrap items-center gap-2 text-sm text-amber-900"
        >
            <Eye className="h-4 w-4 shrink-0" />
            <span className="font-medium flex-1">
                Demo modundasınız. Bu oturumda yapılan değişiklikler kaydedilmez ve düzenleme işlemleri devre dışıdır.
            </span>
            <Link
                to="/login?mode=admin"
                className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-amber-300 bg-white px-3 py-1.5 font-medium text-amber-950 hover:bg-amber-100"
            >
                <ShieldCheck className="h-4 w-4" />
                Admin Modu
            </Link>
        </div>
    );
};
