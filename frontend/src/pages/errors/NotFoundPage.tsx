import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui';

export const NotFoundPage: React.FC = () => {
    return (
        <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md text-center">
                <p className="text-xl font-bold text-blue-600 mb-4">404</p>
                <h2 className="text-3xl font-bold tracking-tight text-gray-900 mb-2">Sayfa Bulunamadı</h2>
                <p className="text-gray-500 mb-8">
                    Aradığınız sayfa bulunamadı.
                </p>
                <Link to="/dashboard">
                    <Button size="lg">Kontrol Paneline Dön</Button>
                </Link>
            </div>
        </div>
    );
};
