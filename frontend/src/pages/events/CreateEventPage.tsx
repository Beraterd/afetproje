import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { createEvent } from '@/api/events.api';
import { getDistricts } from '@/api/districts.api';
import { getNeighborhoods } from '@/api/neighborhoods.api';
import { getTeamTypes } from '@/api/teams.api';
import { useToast } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import { Button, FormField } from '@/components/ui';
import { DistrictResponse, NeighborhoodSummaryResponse, TeamTypeResponse } from '@/types';

const createEventSchema = z.object({
    teamName: z.string().min(1, 'Ekip seçimi zorunludur'),
    districtId: z.string().min(1, 'İlçe seçimi zorunludur'),
    neighborhoodId: z.string().min(1, 'Mahalle seçimi zorunludur'),
    requiredPeople: z.coerce.number().min(1, 'En az 1 kişi girilmelidir'),
    description: z.string().optional(),
    otherTeamDescription: z.string().optional(),
});

type CreateEventFormValues = z.infer<typeof createEventSchema>;

export const CreateEventPage: React.FC = () => {
    const navigate = useNavigate();
    const toast = useToast();
    const queryClient = useQueryClient();
    const user = useAuthStore((s) => s.user);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const [districts, setDistricts] = useState<DistrictResponse[]>([]);
    const [neighborhoods, setNeighborhoods] = useState<NeighborhoodSummaryResponse[]>([]);
    const [teamTypes, setTeamTypes] = useState<TeamTypeResponse[]>([]);

    // Granular loading / error states — team types and districts are independent
    const [teamTypesLoading, setTeamTypesLoading] = useState(true);
    const [districtsLoading, setDistrictsLoading] = useState(true);
    const [neighborhoodsLoading, setNeighborhoodsLoading] = useState(false);
    const [teamTypesError, setTeamTypesError] = useState<string | null>(null);
    const [districtsError, setDistrictsError] = useState<string | null>(null);

    const isAdmin = user?.role === 'ADMIN';
    const isDistrictCoordinator = user?.role === 'DISTRICT_COORDINATOR';
    const isNeighborhoodCoordinator = user?.role === 'NEIGHBORHOOD_COORDINATOR';

    const {
        register,
        handleSubmit,
        watch,
        setValue,
        formState: { errors },
    } = useForm<CreateEventFormValues>({
        resolver: zodResolver(createEventSchema),
        defaultValues: { requiredPeople: 10 },
    });

    const selectedDistrict = watch('districtId');
    const selectedTeamName = watch('teamName');
    const isOtherTeam = selectedTeamName === 'OTHER';

    // ── Load team types (static endpoint, no DB enum mapping) ─────────────────
    useEffect(() => {
        setTeamTypesLoading(true);
        setTeamTypesError(null);
        getTeamTypes()
            .then((types) => setTeamTypes(types))
            .catch((err) => {
                console.error('[CreateEvent] getTeamTypes failed:', err?.response?.status, err?.response?.data);
                setTeamTypesError('Ekip türleri yüklenemedi');
                toast.error('Ekip listesi yüklenemedi. Lütfen sayfayı yenileyin.');
            })
            .finally(() => setTeamTypesLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ── Load districts (independent from team types) ──────────────────────────
    useEffect(() => {
        setDistrictsLoading(true);
        setDistrictsError(null);
        getDistricts()
            .then((d) => {
                if (isAdmin) {
                    setDistricts(d);
                } else if (isDistrictCoordinator) {
                    if (user?.districtId) {
                        const own = d.filter((dist) => dist.id === user.districtId);
                        setDistricts(own.length > 0 ? own : d);
                        setValue('districtId', user.districtId);
                    } else {
                        setDistricts(d);
                        toast.error('Koordinatör ilçe ataması bulunamadı. Lütfen yönetici ile iletişime geçin.');
                    }
                } else if (isNeighborhoodCoordinator) {
                    if (user?.districtId) {
                        const own = d.filter((dist) => dist.id === user.districtId);
                        setDistricts(own.length > 0 ? own : d);
                        setValue('districtId', user.districtId);
                    } else {
                        setDistricts(d);
                        toast.error('Koordinatör ilçe/mahalle ataması bulunamadı. Lütfen yönetici ile iletişime geçin.');
                    }
                } else {
                    setDistricts(d);
                }
            })
            .catch((err) => {
                console.error('[CreateEvent] getDistricts failed:', err?.response?.status, err?.response?.data);
                setDistrictsError('İlçeler yüklenemedi');
                toast.error('İlçe listesi yüklenemedi. Lütfen sayfayı yenileyin.');
            })
            .finally(() => setDistrictsLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ── Load neighborhoods when district changes ───────────────────────────────
    useEffect(() => {
        if (!selectedDistrict) {
            setNeighborhoods([]);
            return;
        }
        setNeighborhoodsLoading(true);
        if (!isNeighborhoodCoordinator) {
            setValue('neighborhoodId', '');
        }
        getNeighborhoods(selectedDistrict)
            .then((list) => {
                setNeighborhoods(list);
                if (isNeighborhoodCoordinator && user?.neighborhoodId) {
                    setValue('neighborhoodId', user.neighborhoodId);
                }
            })
            .catch((err) => {
                console.error('[CreateEvent] getNeighborhoods failed:', err?.response?.status, err?.response?.data);
                toast.error('Mahalle listesi yüklenemedi.');
            })
            .finally(() => setNeighborhoodsLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedDistrict]);

    // ── Submit ────────────────────────────────────────────────────────────────
    const onSubmit = async (data: CreateEventFormValues) => {
        setIsSubmitting(true);
        try {
            let description = data.description || '';
            if (isOtherTeam && data.otherTeamDescription) {
                description = data.otherTeamDescription + (description ? '\n' + description : '');
            }
            const res = await createEvent({
                teamName: data.teamName,          // enum string, e.g. "HASAR_TESPIT_EKIBI"
                neighborhoodId: data.neighborhoodId,
                requiredPeople: data.requiredPeople,
                description: description || undefined,
            });
            queryClient.invalidateQueries({ queryKey: ['map'], exact: false });
            queryClient.invalidateQueries({ queryKey: ['events'], exact: false });
            toast.success('Olay başarıyla oluşturuldu');
            navigate(`/events/${res.id}`);
        } catch (error: any) {
            toast.error(error?.response?.data?.message || error.message || 'Olay oluşturulamadı');
        } finally {
            setIsSubmitting(false);
        }
    };

    // ── Styles ────────────────────────────────────────────────────────────────
    const inputClass =
        'block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6';
    const disabledInputClass =
        'block w-full rounded-md border-0 py-1.5 text-gray-500 bg-gray-100 shadow-sm ring-1 ring-inset ring-gray-300 sm:text-sm sm:leading-6';

    const resolvedDistrictName = (() => {
        if (districtsLoading) return 'Yükleniyor...';
        if (districtsError) return 'Yüklenemedi';
        const found = districts.find((d) => d.id === user?.districtId);
        return found?.name ?? (user?.districtId ? 'İlçe bulunamadı' : 'İlçe atanmamış');
    })();

    const resolvedNeighborhoodName = (() => {
        if (!selectedDistrict) return 'İlçe seçilmedi';
        if (neighborhoodsLoading) return 'Yükleniyor...';
        const found = neighborhoods.find((n) => n.id === user?.neighborhoodId);
        return found?.name ?? (user?.neighborhoodId ? 'Mahalle bulunamadı' : 'Mahalle atanmamış');
    })();

    return (
        <div className="max-w-2xl mx-auto py-6 space-y-6">
            <div>
                <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:tracking-tight">
                    Yeni Olay Oluştur
                </h2>
                {isDistrictCoordinator && (
                    <p className="mt-1 text-sm text-gray-500">
                        Olayı yalnızca kendi ilçeniz için oluşturabilirsiniz.
                    </p>
                )}
                {isNeighborhoodCoordinator && (
                    <p className="mt-1 text-sm text-gray-500">
                        Olayı yalnızca kendi mahalleniz için oluşturabilirsiniz.
                    </p>
                )}
            </div>

            <div className="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 border border-gray-200">
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">

                    {/* 1) İhtiyaç duyulan ekip türü */}
                    <FormField
                        label="İhtiyaç Duyulan Ekip"
                        error={errors.teamName?.message ?? teamTypesError ?? undefined}
                        required
                    >
                        <select
                            {...register('teamName')}
                            disabled={teamTypesLoading}
                            className={`${inputClass} disabled:bg-gray-100 disabled:text-gray-400`}
                        >
                            <option value="">
                                {teamTypesLoading ? 'Ekipler yükleniyor...' : 'Ekip seçin...'}
                            </option>
                            {teamTypes.map((t) => (
                                <option key={t.value} value={t.value}>
                                    {t.label}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    {/* 2) İlçe */}
                    <FormField
                        label="İlçe"
                        error={errors.districtId?.message ?? districtsError ?? undefined}
                        required
                    >
                        {isAdmin ? (
                            <select
                                {...register('districtId')}
                                disabled={districtsLoading}
                                className={`${inputClass} disabled:bg-gray-100 disabled:text-gray-400`}
                                onChange={(e) => {
                                    setValue('districtId', e.target.value);
                                    setValue('neighborhoodId', '');
                                }}
                            >
                                <option value="">
                                    {districtsLoading ? 'İlçeler yükleniyor...' : 'İlçe seçin...'}
                                </option>
                                {districts.map((d) => (
                                    <option key={d.id} value={d.id}>{d.name}</option>
                                ))}
                            </select>
                        ) : (
                            <>
                                <input
                                    type="text"
                                    disabled
                                    value={resolvedDistrictName}
                                    className={disabledInputClass}
                                />
                                <input type="hidden" {...register('districtId')} />
                            </>
                        )}
                    </FormField>

                    {/* 3) Mahalle */}
                    <FormField label="Mahalle" error={errors.neighborhoodId?.message} required>
                        {isNeighborhoodCoordinator ? (
                            <>
                                <input
                                    type="text"
                                    disabled
                                    value={resolvedNeighborhoodName}
                                    className={disabledInputClass}
                                />
                                <input type="hidden" {...register('neighborhoodId')} />
                            </>
                        ) : (
                            <select
                                {...register('neighborhoodId')}
                                disabled={!selectedDistrict || neighborhoodsLoading}
                                className={`${inputClass} disabled:bg-gray-100 disabled:text-gray-400`}
                            >
                                <option value="">
                                    {!selectedDistrict
                                        ? 'Önce ilçe seçin'
                                        : neighborhoodsLoading
                                        ? 'Mahalleler yükleniyor...'
                                        : 'Mahalle seçin...'}
                                </option>
                                {neighborhoods.map((n) => (
                                    <option key={n.id} value={n.id}>{n.name}</option>
                                ))}
                            </select>
                        )}
                    </FormField>

                    {/* 4) İhtiyaç olan kişi sayısı */}
                    <FormField
                        label="İhtiyaç Olan Kişi Sayısı"
                        error={errors.requiredPeople?.message}
                        required
                    >
                        <input
                            {...register('requiredPeople')}
                            type="number"
                            min="1"
                            className={inputClass}
                        />
                    </FormField>

                    {/* 5a) Diğer ekip açıklaması */}
                    {isOtherTeam && (
                        <FormField
                            label="Diğer Ekip Açıklaması"
                            error={errors.otherTeamDescription?.message}
                            required
                        >
                            <textarea
                                {...register('otherTeamDescription')}
                                rows={3}
                                placeholder="İhtiyaç duyulan ekip türünü açıklayın..."
                                className={inputClass}
                            />
                        </FormField>
                    )}

                    {/* 5b) Açıklama (isteğe bağlı) */}
                    <FormField label="Açıklama (isteğe bağlı)" error={errors.description?.message}>
                        <textarea
                            {...register('description')}
                            rows={3}
                            placeholder="Olay hakkında ek bilgi..."
                            className={inputClass}
                        />
                    </FormField>

                    <div className="flex justify-end pt-5 border-t border-gray-200 space-x-3">
                        <Button variant="ghost" type="button" onClick={() => navigate(-1)}>
                            İptal
                        </Button>
                        <Button
                            type="submit"
                            loading={isSubmitting}
                            disabled={isSubmitting}
                        >
                            Olay Oluştur
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
};
