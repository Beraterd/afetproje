import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui';
import { ShieldAlert } from 'lucide-react';

export const UnauthorizedPage: React.FC = () => {
    return (
        <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md text-center">
                <ShieldAlert className="mx-auto h-16 w-16 text-red-500 mb-4" />
                <h2 className="text-3xl font-bold tracking-tight text-gray-900 mb-2">Erişim Reddedildi</h2>
                <p className="text-gray-500 mb-8">
                    Bu sayfaya erişim yetkiniz bulunmuyor. Hata olduğunu düşünüyorsanız sistem yöneticinizle iletişime geçin.
                </p>
                <Link to="/dashboard">
                    <Button size="lg">Kontrol Paneline Dön</Button>
                </Link>
            </div>
        </div>
    );
};
