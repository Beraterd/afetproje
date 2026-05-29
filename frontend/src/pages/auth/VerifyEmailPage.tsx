import React, { useEffect, useState, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { verifyEmail } from '@/api/auth.api';
import { ShieldAlert, CheckCircle, XCircle } from 'lucide-react';
import { Button, LoadingSpinner } from '@/components/ui';

export const VerifyEmailPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');

    const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
    const [errorMessage, setErrorMessage] = useState('');

    // Prevent strict mode double execution on mount
    const hasAttempted = useRef(false);

    useEffect(() => {
        if (!token) {
            setStatus('error');
            setErrorMessage('Doğrulama bağlantısı URL\'de bulunamadı.');
            return;
        }

        if (hasAttempted.current) return;
        hasAttempted.current = true;

        verifyEmail(token)
            .then(() => {
                setStatus('success');
            })
            .catch((error: any) => {
                setStatus('error');
                setErrorMessage(error.message || 'Doğrulama bağlantısı geçersiz veya süresi dolmuş.');
            });
    }, [token]);

    return (
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10 border border-gray-100 flex flex-col items-center text-center">

                {status === 'loading' && (
                    <>
                        <ShieldAlert className="h-12 w-12 text-blue-600 mb-4 animate-pulse" />
                        <h2 className="text-xl font-bold text-gray-900 mb-2">E-posta doğrulanıyor...</h2>
                        <p className="text-gray-500 mb-6">E-posta adresiniz doğrulanırken lütfen bekleyin.</p>
                        <LoadingSpinner size="md" />
                    </>
                )}

                {status === 'success' && (
                    <>
                        <CheckCircle className="h-16 w-16 text-green-500 mb-4" />
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">E-posta Doğrulandı!</h2>
                        <p className="text-gray-500 mb-6">
                            E-posta adresiniz başarıyla doğrulandı. Artık hesabınıza erişebilirsiniz.
                        </p>
                        <Link to="/login" className="w-full">
                            <Button className="w-full" size="lg">Giriş Sayfasına Git</Button>
                        </Link>
                    </>
                )}

                {status === 'error' && (
                    <>
                        <XCircle className="h-16 w-16 text-red-500 mb-4" />
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">Doğrulama Başarısız</h2>
                        <p className="text-gray-500 mb-6">{errorMessage}</p>
                        <Link to="/login" className="w-full">
                            <Button variant="secondary" className="w-full" size="lg">Giriş Sayfasına Dön</Button>
                        </Link>
                    </>
                )}

            </div>
        </div>
    );
};
