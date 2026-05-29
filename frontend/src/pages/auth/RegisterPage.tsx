import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { register as registerUser } from '@/api/auth.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import { useToast } from '@/components/shared/ToastProvider';
import { Button, FormField } from '@/components/ui';
import { ShieldAlert } from 'lucide-react';
import { DistrictResponse, NeighborhoodSummaryResponse } from '@/types';
import { ApiError } from '@/utils/errorParser';

const registerSchema = z.object({
    firstName: z.string().min(2, 'Ad en az 2 karakter olmalıdır'),
    lastName: z.string().min(2, 'Soyad en az 2 karakter olmalıdır'),
    email: z.string().email('Geçerli bir e-posta adresi girin'),
    password: z.string().min(8, 'Şifre en az 8 karakter olmalıdır'),
    phone: z.string().min(1, 'Telefon numarası zorunludur').max(20, 'Telefon numarası çok uzun'),
    bloodType: z.string().min(1, 'Kan grubu seçimi zorunludur'),
    districtId: z.string().min(1, 'İlçe seçimi zorunludur'),
    neighborhoodId: z.string().min(1, 'Mahalle seçimi zorunludur'),
    address: z.string().min(1, 'Adres zorunludur'),
    profession: z.string().optional(),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
    const navigate = useNavigate();
    const toast = useToast();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [districts, setDistricts] = useState<DistrictResponse[]>([]);
    const [neighborhoods, setNeighborhoods] = useState<NeighborhoodSummaryResponse[]>([]);

    const {
        register,
        handleSubmit,
        watch,
        setValue,
        formState: { errors },
    } = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
    });

    const selectedDistrict = watch('districtId');

    useEffect(() => {
        getDistricts().then(setDistricts).catch(() => toast.error('İlçeler yüklenemedi'));
    }, []);

    useEffect(() => {
        if (selectedDistrict) {
            setValue('neighborhoodId', '');
            getNeighborhoods(selectedDistrict)
                .then(setNeighborhoods)
                .catch(() => toast.error('Mahalleler yüklenemedi'));
        } else {
            setNeighborhoods([]);
        }
    }, [selectedDistrict]);

    const onSubmit = async (data: RegisterFormValues) => {
        setIsSubmitting(true);
        try {
            await registerUser(data);
            toast.success('Kayıt başarılı!');
            navigate('/login');
        } catch (error) {
            const apiError = error as ApiError;
            if (apiError.status === 400 && apiError.details) {
                toast.error('Lütfen formdaki hataları düzeltin.');
            } else {
                toast.error(apiError.message || 'Kayıt başarısız oldu.');
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-xl">
            <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10 border border-gray-100">
                <div className="sm:mx-auto sm:w-full sm:max-w-md mb-6 flex flex-col items-center">
                    <ShieldAlert className="h-10 w-10 text-blue-600 mb-2" />
                    <h2 className="text-center text-2xl font-bold tracking-tight text-gray-900">
                        Gönüllü Olarak Kayıt Ol
                    </h2>
                </div>

                <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <FormField label="Ad" error={errors.firstName?.message} required>
                            <input
                                {...register('firstName')}
                                type="text"
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                            />
                        </FormField>

                        <FormField label="Soyad" error={errors.lastName?.message} required>
                            <input
                                {...register('lastName')}
                                type="text"
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                            />
                        </FormField>
                    </div>

                    <FormField label="E-posta Adresi" error={errors.email?.message} required>
                        <input
                            {...register('email')}
                            type="email"
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                        />
                    </FormField>

                    <FormField label="Şifre" error={errors.password?.message} required hint="En az 8 karakter">
                        <input
                            {...register('password')}
                            type="password"
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                        />
                    </FormField>

                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <FormField label="Telefon" error={errors.phone?.message} required hint="Örn: +905XXXXXXXXX">
                            <input
                                {...register('phone')}
                                type="tel"
                                placeholder="+905XXXXXXXXX"
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                            />
                        </FormField>

                        <FormField label="Kan Grubu" error={errors.bloodType?.message} required>
                            <select
                                {...register('bloodType')}
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                            >
                                <option value="">Kan grubu seçin...</option>
                                <option value="A_POSITIVE">A Rh+</option>
                                <option value="A_NEGATIVE">A Rh-</option>
                                <option value="B_POSITIVE">B Rh+</option>
                                <option value="B_NEGATIVE">B Rh-</option>
                                <option value="AB_POSITIVE">AB Rh+</option>
                                <option value="AB_NEGATIVE">AB Rh-</option>
                                <option value="O_POSITIVE">0 Rh+</option>
                                <option value="O_NEGATIVE">0 Rh-</option>
                            </select>
                        </FormField>
                    </div>

                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <FormField label="İlçe" error={errors.districtId?.message} required>
                            <select
                                {...register('districtId')}
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                onChange={(e) => {
                                    setValue('districtId', e.target.value);
                                    setValue('neighborhoodId', '');
                                }}
                            >
                                <option value="">İlçe seçin...</option>
                                {districts.map((d) => (
                                    <option key={d.id} value={d.id}>{d.name}</option>
                                ))}
                            </select>
                        </FormField>

                        <FormField label="Mahalle" error={errors.neighborhoodId?.message} required>
                            <select
                                {...register('neighborhoodId')}
                                disabled={!selectedDistrict}
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm disabled:bg-gray-100"
                            >
                                <option value="">Mahalle seçin...</option>
                                {neighborhoods.map((n) => (
                                    <option key={n.id} value={n.id}>{n.name}</option>
                                ))}
                            </select>
                        </FormField>
                    </div>

                    <FormField label="Açık Adres" error={errors.address?.message} required>
                        <input
                            {...register('address')}
                            type="text"
                            placeholder="Mahalle, cadde, sokak, bina no..."
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                        />
                    </FormField>

                    <div>
                        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
                            Kayıt Ol
                        </Button>
                    </div>
                </form>

                <div className="mt-6 text-center text-sm">
                    <span className="text-gray-500">Zaten hesabınız var mı? </span>
                    <Link to="/login" className="font-semibold text-blue-600 hover:text-blue-500">
                        Giriş Yap
                    </Link>
                </div>
            </div>
        </div>
    );
};
