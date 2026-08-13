import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/api/buildings.api', () => ({ getBuilding3dConfig: vi.fn() }));
vi.mock('@/utils/webgl', () => ({ isWebglSupported: vi.fn() }));
vi.mock('@/components/damage/BuildingSelector', () => ({
    BuildingSelector: () => <div data-testid="building-selector-stub" />,
}));
vi.mock('@/components/map/LocationPickerMap', () => ({
    LocationPickerMap: () => <div data-testid="location-picker-stub" />,
}));

import { getBuilding3dConfig } from '@/api/buildings.api';
import { isWebglSupported } from '@/utils/webgl';
import { DamageAssessmentCreateModal, DamageAssessmentFormState } from '@/components/damage/DamageAssessmentCreateModal';

const PENDIK = { id: 'pendik-id', name: 'Pendik', riskScore: 1, riskColor: 'GREEN', riskScoreUpdatedAt: '' } as any;
const KADIKOY = { id: 'kadikoy-id', name: 'Kadıköy', riskScore: 1, riskColor: 'GREEN', riskScoreUpdatedAt: '' } as any;
const NEIGHBORHOODS = [{ id: 'nb-1', name: 'Kurtköy', districtName: 'Pendik' }];

function renderModal(overrides: { selectedDistrictId: string; form?: DamageAssessmentFormState }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <DamageAssessmentCreateModal
                showDistrictSelect
                districts={[PENDIK, KADIKOY]}
                neighborhoods={NEIGHBORHOODS}
                selectedDistrictId={overrides.selectedDistrictId}
                onDistrictChange={vi.fn()}
                form={overrides.form ?? {}}
                setForm={vi.fn()}
                onNeighborhoodChange={vi.fn()}
                onPositionChange={vi.fn()}
                locationConfirmed={false}
                onConfirmLocation={vi.fn()}
                photos={[]}
                photoErrors={[]}
                photoInputRef={{ current: null }}
                onPhotoSelect={vi.fn()}
                onRemovePhoto={vi.fn()}
                creating={false}
                onCancel={vi.fn()}
                onSubmit={vi.fn()}
            />
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    (getBuilding3dConfig as any).mockResolvedValue({
        enabledDistricts: ['Pendik'], buildingMinZoom: 15, extrusionMinZoom: 16, totalBuildingCount: 10,
    });
});

describe('DamageAssessmentCreateModal — bina konumu seçici yönlendirmesi', () => {
    it('Pendik seçili + WebGL destekleniyorsa BuildingSelector gösterilir, manuel mahalle select gizlenir', async () => {
        (isWebglSupported as any).mockReturnValue(true);
        renderModal({ selectedDistrictId: 'pendik-id' });

        expect(await screen.findByTestId('building-selector-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('location-picker-stub')).not.toBeInTheDocument();
        expect(screen.getByText(/haritadan bina seçtiğinizde otomatik doldurulur/i)).toBeInTheDocument();
    });

    it('Pendik seçili ama WebGL desteklenmiyorsa manuel akışa (LocationPickerMap benzeri uyarı) düşer', async () => {
        (isWebglSupported as any).mockReturnValue(false);
        renderModal({ selectedDistrictId: 'pendik-id', form: { neighborhoodId: 'nb-1' } });

        expect(await screen.findByText(/3B bina görünümü bu cihazda kullanılamıyor/i)).toBeInTheDocument();
        expect(screen.getByTestId('location-picker-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('building-selector-stub')).not.toBeInTheDocument();
    });

    it('3B-uygun olmayan bir ilçe seçiliyse her zaman manuel Mahalle select + LocationPickerMap kullanılır', async () => {
        (isWebglSupported as any).mockReturnValue(true);
        renderModal({ selectedDistrictId: 'kadikoy-id', form: { neighborhoodId: 'nb-1' } });

        await screen.findByTestId('location-picker-stub');
        expect(screen.queryByText(/haritadan bina seçtiğinizde otomatik doldurulur/i)).not.toBeInTheDocument();
        expect(screen.queryByTestId('building-selector-stub')).not.toBeInTheDocument();
    });

    it('config enabledDistricts Kadıköy (Pendik-dışı bir ilçe) içerince BuildingSelector orada da gösterilir', async () => {
        // Eligibility tamamen building3dConfig.enabledDistricts.includes(districtName)'e dayanır —
        // kod içinde hiçbir yerde "Pendik" hardcode edilmediğini kanıtlar (bkz. DamageAssessmentCreateModal.tsx).
        (getBuilding3dConfig as any).mockResolvedValue({
            enabledDistricts: ['Kadıköy'], buildingMinZoom: 15, extrusionMinZoom: 16, totalBuildingCount: 5000,
        });
        (isWebglSupported as any).mockReturnValue(true);
        renderModal({ selectedDistrictId: 'kadikoy-id' });

        expect(await screen.findByTestId('building-selector-stub')).toBeInTheDocument();
        expect(screen.queryByTestId('location-picker-stub')).not.toBeInTheDocument();
    });
});
