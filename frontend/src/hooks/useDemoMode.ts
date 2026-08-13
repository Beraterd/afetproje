import { useAuthStore } from '@/store/authStore';

export const DEMO_MODE_DISABLED_REASON = 'Demo modunda düzenleme yapılamaz.';

/**
 * "Admin Demo Modu" ziyaretçi hesabı için merkezi tespit noktası — write aksiyonlarını
 * backend'den 403 gelmeden önce UI seviyesinde disable etmek için kullanılır.
 * Backend'deki DemoModeWriteGuardFilter zaten son çare koruma; bu yalnızca UX iyileştirmesidir.
 */
export function useDemoMode() {
    const isDemo = useAuthStore((s) => !!s.user?.demo);
    return {
        isDemo,
        disabledReason: isDemo ? DEMO_MODE_DISABLED_REASON : undefined,
    };
}
