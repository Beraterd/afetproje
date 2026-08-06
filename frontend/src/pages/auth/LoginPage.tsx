import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { login, demoLogin } from '@/api/auth.api';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/components/shared/ToastProvider';
import { Button, FormField } from '@/components/ui';
import { ShieldAlert as ShieldIcon, Eye as EyeIcon } from 'lucide-react';
import { ApiError } from '@/utils/errorParser';

const loginSchema = z.object({
    emailOrUsername: z.string().min(1, 'E-posta veya kullanıcı adı gereklidir'),
    password: z.string().min(1, 'Şifre gereklidir'),
});
type LoginValues = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
    const navigate = useNavigate();
    const { setAuth } = useAuthStore();
    const toast = useToast();
    const [isDemoLoading, setIsDemoLoading] = useState(false);

    const {
        register,
        handleSubmit,
        setError,
        formState: { errors, isSubmitting },
    } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) });

    const onSubmit = async (data: LoginValues) => {
        try {
            const res = await login(data);
            setAuth(res.token, res.user);
            localStorage.setItem('afet_token', res.token);
            toast.success('Başarıyla giriş yapıldı');
            navigate('/dashboard');
        } catch (error) {
            const apiError = error as ApiError;
            if (apiError.status === 401) {
                setError('root', { message: 'Kullanıcı adı/e-posta veya şifre hatalı' });
            } else if (apiError.status === 403) {
                setError('root', { message: 'Hesabınız devre dışı bırakılmış' });
            } else {
                setError('root', { message: apiError.message || 'Giriş başarısız oldu' });
            }
        }
    };

    const handleDemoLogin = async () => {
        if (isDemoLoading) return; // arka arkaya tıklamayı engelle
        setIsDemoLoading(true);
        try {
            const res = await demoLogin();
            setAuth(res.token, res.user);
            localStorage.setItem('afet_token', res.token);
            toast.info('Demo modundasınız. Bu oturumda yapılan değişiklikler kaydedilmez ve düzenleme işlemleri devre dışıdır.');
            navigate('/dashboard');
        } catch (error) {
            const apiError = error as ApiError;
            toast.error(apiError.message || 'Demo modu şu anda başlatılamadı.');
        } finally {
            setIsDemoLoading(false);
        }
    };

    return (
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10 border border-gray-100">

                <div className="sm:mx-auto sm:w-full sm:max-w-md mb-6 flex flex-col items-center">
                    <ShieldIcon className="h-12 w-12 text-blue-600 mb-2" />
                    <h2 className="text-center text-2xl font-bold tracking-tight text-gray-900">
                        Hesabınıza Giriş Yapın
                    </h2>
                    <p className="mt-1 text-sm text-gray-500">İstanbul Afet Koordinasyon Sistemi</p>
                </div>

                <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                    <FormField label="E-posta veya Kullanıcı Adı" error={errors.emailOrUsername?.message} required>
                        <input
                            {...register('emailOrUsername')}
                            type="text"
                            autoComplete="username"
                            className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                        />
                    </FormField>

                    <FormField label="Şifre" error={errors.password?.message} required>
                        <input
                            {...register('password')}
                            type="password"
                            autoComplete="current-password"
                            className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                        />
                    </FormField>

                    <div className="flex justify-end">
                        <Link to="/forgot-password" className="text-sm text-blue-600 hover:text-blue-500">
                            Şifreni unuttun mu?
                        </Link>
                    </div>

                    {errors.root?.message && (
                        <div className="rounded-md bg-red-50 p-4">
                            <p className="text-sm font-medium text-red-800">{errors.root.message}</p>
                        </div>
                    )}

                    <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
                        Giriş Yap
                    </Button>
                </form>

                <div className="mt-6">
                    <div className="relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-dashed border-gray-300" />
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="bg-white px-2 text-gray-500">veya</span>
                        </div>
                    </div>
                    <div className="mt-4">
                        <Button
                            type="button"
                            variant="secondary"
                            size="lg"
                            className="w-full border-dashed"
                            leftIcon={<EyeIcon className="h-4 w-4" />}
                            loading={isDemoLoading}
                            onClick={handleDemoLogin}
                            aria-label="Giriş yapmadan salt okunur admin demo modunu başlat"
                        >
                            Giriş Yapmadan Admin Olarak Siteyi Gez
                        </Button>
                        <p className="mt-2 text-center text-xs text-gray-400">
                            Salt okunur demo modu — hiçbir veri değiştirilemez.
                        </p>
                    </div>
                </div>

                <div className="mt-6">
                    <div className="relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-300" />
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="bg-white px-2 text-gray-500">Henüz gönüllü hesabınız yok mu?</span>
                        </div>
                    </div>
                    <div className="mt-6">
                        <Link
                            to="/register"
                            className="flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                        >
                            Gönüllü Olarak Kayıt Ol
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
};
