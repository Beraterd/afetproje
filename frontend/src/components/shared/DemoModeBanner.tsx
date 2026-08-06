import React from 'react';
import { Eye } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

export const DemoModeBanner: React.FC = () => {
    const isDemo = useAuthStore((s) => s.user?.demo);

    if (!isDemo) return null;

    return (
        <div
            role="status"
            className="bg-amber-50 border-b border-amber-200 px-4 py-2 flex items-center gap-2 text-sm text-amber-900"
        >
            <Eye className="h-4 w-4 shrink-0" />
            <span className="font-medium">
                Demo modundasınız. Bu oturumda yapılan değişiklikler kaydedilmez ve düzenleme işlemleri devre dışıdır.
            </span>
        </div>
    );
};
