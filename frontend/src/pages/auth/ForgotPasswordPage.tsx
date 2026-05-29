import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { forgotPassword } from '@/api/auth.api';
import { Button, FormField } from '@/components/ui';
import { ShieldAlert as ShieldIcon } from 'lucide-react';

const schema = z.object({
    email: z.string().email('Geçerli bir e-posta adresi girin'),
});
type FormValues = z.infer<typeof schema>;

export const ForgotPasswordPage: React.FC = () => {
    const [submitted, setSubmitted] = useState(false);

    const {
        register,
        handleSubmit,
        setError,
        formState: { errors, isSubmitting },
    } = useForm<FormValues>({ resolver: zodResolver(schema) });

    const onSubmit = async (data: FormValues) => {
        try {
            await forgotPassword(data.email);
            setSubmitted(true);
        } catch {
            setError('root', { message: 'Bir hata oluştu. Lütfen tekrar deneyin.' });
        }
    };

    return (
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10 border border-gray-100">
                <div className="sm:mx-auto sm:w-full sm:max-w-md mb-6 flex flex-col items-center">
                    <ShieldIcon className="h-12 w-12 text-blue-600 mb-2" />
                    <h2 className="text-center text-2xl font-bold tracking-tight text-gray-900">
                        Şifremi Unuttum
                    </h2>
                    <p className="mt-1 text-sm text-gray-500">İstanbul Afet Koordinasyon Sistemi</p>
                </div>

                {submitted ? (
                    <div className="rounded-md bg-green-50 p-4 text-center">
                        <p className="text-sm font-medium text-green-800">
                            Eğer bu e-posta sisteme kayıtlıysa şifre sıfırlama bağlantısı gönderildi.
                        </p>
                        <p className="mt-2 text-sm text-gray-600">
                            Lütfen e-postanızı kontrol edin. Bağlantı 60 dakika geçerlidir.
                        </p>
                        <Link
                            to="/login"
                            className="mt-4 inline-block text-sm text-blue-600 hover:text-blue-500"
                        >
                            Giriş sayfasına dön
                        </Link>
                    </div>
                ) : (
                    <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                        <p className="text-sm text-gray-600">
                            Kayıtlı e-posta adresinizi girin. Şifre sıfırlama bağlantısı gönderilecektir.
                        </p>

                        <FormField label="E-posta Adresi" error={errors.email?.message} required>
                            <input
                                {...register('email')}
                                type="email"
                                autoComplete="email"
                                className="block w-full rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                            />
                        </FormField>

                        {errors.root?.message && (
                            <div className="rounded-md bg-red-50 p-4">
                                <p className="text-sm font-medium text-red-800">{errors.root.message}</p>
                            </div>
                        )}

                        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
                            Sıfırlama Bağlantısı Gönder
                        </Button>

                        <div className="text-center">
                            <Link to="/login" className="text-sm text-blue-600 hover:text-blue-500">
                                Giriş sayfasına dön
                            </Link>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
};
