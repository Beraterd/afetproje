import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { getMe, updateMe } from '@/api/users.api';
import { getDistricts, getNeighborhoodsByDistrict } from '@/api/districts.api';
import { getNotificationPreferences, updateNotificationPreferences } from '@/api/notificationPreferences.api';
import { Button, FormField, Badge, LoadingSpinner } from '@/components/ui';
import { UserResponse, DistrictResponse, NeighborhoodSummaryResponse, NotificationPreferences } from '@/types';
import { ApiError } from '@/utils/errorParser';
import { isValidTurkishMobile, PHONE_HINT, PHONE_PLACEHOLDER } from '@/utils/phone';
import { Bell } from 'lucide-react';

const ROLE_TR: Record<string, string> = {
    ADMIN: 'Yönetici',
    DISTRICT_COORDINATOR: 'İlçe Koordinatörü',
    NEIGHBORHOOD_COORDINATOR: 'Mahalle Koordinatörü',
    VOLUNTEER: 'Gönüllü',
};

const profileSchema = z.object({
    firstName: z.string().min(2, 'Ad en az 2 karakter olmalıdır'),
    lastName: z.string().min(2, 'Soyad en az 2 karakter olmalıdır'),
    email: z.string().email('Geçerli bir e-posta adresi giriniz'),
    phone: z.string()
        .optional()
        .refine(v => !v || v.trim() === '' || isValidTurkishMobile(v),
            'Geçerli bir telefon girin: ' + PHONE_HINT),
    bloodType: z.string().optional(),
    address: z.string().optional(),
    profession: z.string().optional(),
    districtId: z.string().min(1, 'İlçe seçiniz'),
    neighborhoodId: z.string().min(1, 'Mahalle seçiniz'),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export const ProfilePage: React.FC = () => {
    const { user, setAuth, accessToken } = useAuthStore();
    const toast = useToast();

    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [profile, setProfile] = useState<UserResponse | null>(null);
    const [districts, setDistricts] = useState<DistrictResponse[]>([]);
    const [neighborhoods, setNeighborhoods] = useState<NeighborhoodSummaryResponse[]>([]);
    const [loadingNeighborhoods, setLoadingNeighborhoods] = useState(false);
    const [hasLoadedInitialData, setHasLoadedInitialData] = useState(false);
    const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
    const [savingPrefs, setSavingPrefs] = useState(false);

    const {
        register,
        handleSubmit,
        reset,
        setValue,
        watch,
        formState: { errors, isDirty },
    } = useForm<ProfileFormValues>({
        resolver: zodResolver(profileSchema),
    });

    const watchedDistrictId = watch('districtId');

    useEffect(() => {
        if (!hasLoadedInitialData) {
            return;
        }

        if (!watchedDistrictId) {
            setNeighborhoods([]);
            return;
        }

        setLoadingNeighborhoods(true);
        getNeighborhoodsByDistrict(watchedDistrictId)
            .then(setNeighborhoods)
            .catch(() => toast.error('Mahalle listesi yüklenemedi'))
            .finally(() => setLoadingNeighborhoods(false));
    }, [hasLoadedInitialData, watchedDistrictId]);

    useEffect(() => {
        let cancelled = false;

        const initializeProfile = async () => {
            try {
                const [districtList, data, prefsData] = await Promise.all([getDistricts(), getMe(), getNotificationPreferences()]);
                if (cancelled) return;

                setDistricts(districtList);
                setProfile(data);
                setPrefs(prefsData);
                reset({
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    phone: data.phone || '',
                    bloodType: data.bloodType || '',
                    address: data.address || '',
                    profession: data.profession || '',
                    districtId: data.district?.id || '',
                    neighborhoodId: data.neighborhood?.id || '',
                });

                if (data.district?.id) {
                    try {
                        const initialNeighborhoods = await getNeighborhoodsByDistrict(data.district.id);
                        if (!cancelled) {
                            setNeighborhoods(initialNeighborhoods);
                        }
                    } catch {
                        toast.error('Mahalle listesi yüklenemedi');
                    }
                } else {
                    setNeighborhoods([]);
                }

                if (!cancelled) {
                    setHasLoadedInitialData(true);
                }
            } catch {
                toast.error('Profil bilgileri yüklenemedi');
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };

        initializeProfile();

        return () => {
            cancelled = true;
        };
    }, [reset]);

    // İlçe select için: RHF onChange'i koru + neighborhoodId'yi sıfırla
    const districtField = register('districtId');

    const handleDistrictChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        districtField.onChange(e);
        setValue('neighborhoodId', '', { shouldDirty: true });
    };

    const onSubmit = async (data: ProfileFormValues) => {
        setIsSaving(true);
        try {
            const updated = await updateMe(data);
            setProfile(updated);

            if (user && accessToken) {
                const changed =
                    user.firstName !== updated.firstName ||
                    user.lastName !== updated.lastName ||
                    user.email !== updated.email;
                if (changed) {
                    setAuth(accessToken, {
                        ...user,
                        firstName: updated.firstName,
                        lastName: updated.lastName,
                        email: updated.email,
                    });
                }
            }

            toast.success('Profiliniz başarıyla güncellendi');
            reset(data);
        } catch (error) {
            const apiError = error as ApiError;
            toast.error(apiError.message || 'Profil güncellenemedi');
        } finally {
            setIsSaving(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex justify-center py-12">
                <LoadingSpinner />
            </div>
        );
    }

    return (
        <div className="max-w-3xl mx-auto py-6">
            <div className="md:flex md:items-center md:justify-between mb-8">
                <div className="min-w-0 flex-1">
                    <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:text-3xl sm:tracking-tight">
                        Profilim
                    </h2>
                </div>
            </div>

            <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
                <div className="px-4 py-5 sm:px-6 flex justify-between items-center border-b border-gray-200">
                    <div>
                        <h3 className="text-lg leading-6 font-medium text-gray-900">Kişisel Bilgiler</h3>
                        <p className="mt-1 max-w-2xl text-sm text-gray-500">Bilgilerinizi görüntüleyin ve güncelleyin.</p>
                    </div>
                    <Badge variant="info">{ROLE_TR[user?.role || ''] || user?.role}</Badge>
                </div>

                <div className="px-4 py-5 sm:p-6">
                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">

                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                            <FormField label="Ad" error={errors.firstName?.message} required>
                                <input
                                    {...register('firstName')}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                />
                            </FormField>

                            <FormField label="Soyad" error={errors.lastName?.message} required>
                                <input
                                    {...register('lastName')}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                />
                            </FormField>

                            <FormField label="E-posta" error={errors.email?.message} required>
                                <input
                                    type="email"
                                    {...register('email')}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                />
                            </FormField>

                            <FormField label="Telefon" error={errors.phone?.message} hint={'Örn: ' + PHONE_HINT}>
                                <input
                                    {...register('phone')}
                                    type="tel"
                                    placeholder={PHONE_PLACEHOLDER}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                />
                            </FormField>

                            <FormField label="Kan Grubu" error={errors.bloodType?.message}>
                                <select
                                    {...register('bloodType')}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                >
                                    <option value="">Bilinmiyor</option>
                                    <option value="A_POSITIVE">A+</option>
                                    <option value="A_NEGATIVE">A-</option>
                                    <option value="B_POSITIVE">B+</option>
                                    <option value="B_NEGATIVE">B-</option>
                                    <option value="AB_POSITIVE">AB+</option>
                                    <option value="AB_NEGATIVE">AB-</option>
                                    <option value="O_POSITIVE">O+</option>
                                    <option value="O_NEGATIVE">O-</option>
                                </select>
                            </FormField>

                            <FormField label="Meslek" error={errors.profession?.message}>
                                <input
                                    {...register('profession')}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                />
                            </FormField>
                        </div>

                        <FormField label="Adres" error={errors.address?.message}>
                            <textarea
                                {...register('address')}
                                rows={3}
                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                            />
                        </FormField>

                        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                            <FormField label="İlçe" error={errors.districtId?.message} required>
                                <select
                                    name={districtField.name}
                                    ref={districtField.ref}
                                    onBlur={districtField.onBlur}
                                    onChange={handleDistrictChange}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm"
                                >
                                    <option value="">İlçe seçiniz</option>
                                    {districts.map((d) => (
                                        <option key={d.id} value={d.id}>{d.name}</option>
                                    ))}
                                </select>
                            </FormField>

                            <FormField label="Mahalle" error={errors.neighborhoodId?.message} required>
                                <select
                                    {...register('neighborhoodId')}
                                    disabled={!watchedDistrictId || loadingNeighborhoods}
                                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 disabled:bg-gray-50 disabled:text-gray-500 sm:text-sm"
                                >
                                    <option value="">
                                        {loadingNeighborhoods ? 'Yükleniyor...' : 'Mahalle seçiniz'}
                                    </option>
                                    {neighborhoods.map((n) => (
                                        <option key={n.id} value={n.id}>{n.name}</option>
                                    ))}
                                </select>
                            </FormField>
                        </div>

                        <div className="pt-5 border-t border-gray-200 flex justify-end">
                            <Button type="submit" loading={isSaving} disabled={!isDirty}>
                                Değişiklikleri Kaydet
                            </Button>
                        </div>

                    </form>
                </div>
            </div>

            {/* Email Bildirim Tercihleri */}
            {prefs && (
                <div className="mt-6 bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
                    <div className="px-4 py-5 sm:px-6 border-b border-gray-200 flex items-center gap-2">
                        <Bell className="h-5 w-5 text-blue-600" />
                        <div>
                            <h3 className="text-lg leading-6 font-medium text-gray-900">E-posta Bildirim Tercihleri</h3>
                            <p className="mt-0.5 text-sm text-gray-500">
                                Hangi konularda e-posta almak istediğinizi seçin.
                            </p>
                        </div>
                    </div>
                    <div className="px-4 py-5 sm:p-6 space-y-4">
                        {[
                            { key: 'emailTaskNotificationsEnabled', label: 'Görev bildirimlerini e-posta ile almak istiyorum' },
                            { key: 'emailDamageNotificationsEnabled', label: 'Hasar tespit bildirimlerini e-posta ile almak istiyorum' },
                            { key: 'emailEarthquakeNotificationsEnabled', label: 'Deprem ve afet bilgilendirmelerini e-posta ile almak istiyorum' },
                            { key: 'emailTeamNotificationsEnabled', label: 'Ekip ve operasyon bildirimlerini e-posta ile almak istiyorum' },
                            { key: 'emailAidNotificationsEnabled', label: 'Yardım talebi bildirimlerini e-posta ile almak istiyorum' },
                            { key: 'emailSystemNotificationsEnabled', label: 'Sistem ve admin bildirimlerini e-posta ile almak istiyorum' },
                        ].map(({ key, label }) => (
                            <label key={key} className="flex items-center gap-3 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={prefs[key as keyof NotificationPreferences]}
                                    onChange={(e) => setPrefs({ ...prefs, [key]: e.target.checked })}
                                    className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-600"
                                />
                                <span className="text-sm text-gray-700">{label}</span>
                            </label>
                        ))}

                        <div className="mt-4 rounded-md bg-gray-50 p-3 text-sm text-gray-500">
                            Hesap güvenliği, e-posta doğrulama ve şifre sıfırlama bildirimleri her zaman gönderilir.
                        </div>

                        <div className="pt-3 flex justify-end">
                            <Button
                                type="button"
                                loading={savingPrefs}
                                onClick={async () => {
                                    setSavingPrefs(true);
                                    try {
                                        const updated = await updateNotificationPreferences(prefs);
                                        setPrefs(updated);
                                        toast.success('Bildirim tercihleriniz kaydedildi');
                                    } catch {
                                        toast.error('Tercihler kaydedilemedi');
                                    } finally {
                                        setSavingPrefs(false);
                                    }
                                }}
                            >
                                Tercihleri Kaydet
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
