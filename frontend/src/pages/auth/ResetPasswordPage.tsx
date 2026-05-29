import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { validateResetToken, resetPassword } from '@/api/auth.api';
import { Button, FormField } from '@/components/ui';
import { ShieldAlert as ShieldIcon } from 'lucide-react';

const schema = z
    .object({
        newPassword: z.string().min(8, 'Şifre en az 8 karakter olmalıdır'),
        confirmPassword: z.string().min(1, 'Şifre tekrarı gereklidir'),
    })
    .refine((d) => d.newPassword === d.confirmPassword, {
        message: 'Şifreler eşleşmiyor',
        path: ['confirmPassword'],
    });
type FormValues = z.infer<typeof schema>;

type TokenState = 'checking' | 'valid' | 'invalid';

export const ResetPasswordPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token') ?? '';
    const [tokenState, setTokenState] = useState<TokenState>('checking');
    const [success, setSuccess] = useState(false);

    const {
        register,
        handleSubmit,
        setError,
        formState: { errors, isSubmitting },
    } = useForm<FormValues>({ resolver: zodResolver(schema) });

    useEffect(() => {
        if (!token) {
            setTokenState('invalid');
            return;
        }
        validateResetToken(token)
            .then((res) => setTokenState(res.valid ? 'valid' : 'invalid'))
            .catch(() => setTokenState('invalid'));
    }, [token]);

    const onSubmit = async (data: FormValues) => {
        try {
            await resetPassword(token, data.newPassword, data.confirmPassword);
            setSuccess(true);
            setTimeout(() => navigate('/login'), 3000);
        } catch (err: any) {
            const msg = err?.response?.data?.message || 'Bir hata oluştu. Lütfen tekrar deneyin.';
            setError('root', { message: msg });
        }
    };

    return (
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10 border border-gray-100">
                <div className="sm:mx-auto sm:w-full sm:max-w-md mb-6 flex flex-col items-center">
                    <ShieldIcon className="h-12 w-12 text-blue-600 mb-2" />
                    <h2 className="text-center text-2xl font-bold tracking-tight text-gray-900">
                        Yeni Şifre Belirle
                    </h2>
                    <p className="mt-1 text-sm text-gray-500">İstanbul Afet Koordinasyon Sistemi</p>
                </div>

                {tokenState === 'checking' && (
                    <p className="text-center text-sm text-gray-500">Bağlantı doğrulanıyor...</p>
                )}

                {tokenState === 'invalid' && (
                    <div className="rounded-md bg-red-50 p-4 text-center">
                        <p className="text-sm font-medium text-red-800">
                            Bu bağlantı geçersiz veya süresi dolmuş.
                        </p>
                        <Link
                            to="/forgot-password"
                            className="mt-3 inline-block text-sm text-blue-600 hover:text-blue-500"
                        >
                            Yeni sıfırlama bağlantısı talep et
                        </Link>
                    </div>
                )}

                {tokenState === 'valid' && success && (
                    <div className="rounded-md bg-green-50 p-4 text-center">
                        <p className="text-sm font-medium text-green-800">
                            Şifreniz başarıyla güncellendi.
                        </p>
                        <p className="mt-2 text-sm text-gray-600">
                            Giriş sayfasına yönlendiriliyorsunuz...
                        </p>
                    </div>
                )}

                {tokenState === 'valid' && !success && (
                    <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                        <FormField label="Yeni Şifre" error={errors.newPassword?.message} required>
                            <input
                                {...register('newPassword')}
                                type="password"
                                autoComplete="new-password"
                                className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                            />
                        </FormField>

                        <FormField label="Şifre Tekrarı" error={errors.confirmPassword?.message} required>
                            <input
                                {...register('confirmPassword')}
                                type="password"
                                autoComplete="new-password"
                                className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                            />
                        </FormField>

                        {errors.root?.message && (
                            <div className="rounded-md bg-red-50 p-4">
                                <p className="text-sm font-medium text-red-800">{errors.root.message}</p>
                            </div>
                        )}

                        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
                            Şifremi Güncelle
                        </Button>
                    </form>
                )}
            </div>
        </div>
    );
};
