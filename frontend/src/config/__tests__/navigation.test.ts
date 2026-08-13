import { describe, it, expect } from 'vitest';
import { getVisibleNavGroups, findActiveGroupId, NAV_GROUPS } from '@/config/navigation';

function flatLabels(groups: ReturnType<typeof getVisibleNavGroups>): string[] {
    return groups.flatMap((g) => g.items.map((i) => i.label));
}

describe('navigation config', () => {
    it('admin sees every group with its full item set', () => {
        const groups = getVisibleNavGroups('ADMIN');
        const groupIds = groups.map((g) => g.id);

        expect(groupIds).toEqual(NAV_GROUPS.map((g) => g.id));
        expect(flatLabels(groups)).toContain('Kullanıcı Yönetimi');
        expect(flatLabels(groups)).toContain('Sistem Bakımı');
        expect(flatLabels(groups)).toContain('İşlem Kayıtları');
    });

    it('volunteer does not see admin-only items or the groups that become empty', () => {
        const groups = getVisibleNavGroups('VOLUNTEER');
        const labels = flatLabels(groups);

        expect(labels).not.toContain('Kullanıcı Yönetimi');
        expect(labels).not.toContain('Sistem Bakımı');
        expect(labels).not.toContain('İşlem Kayıtları');
        expect(labels).not.toContain('Belge Onayları');
        expect(labels).not.toContain('Koordinatör Atamaları');
        expect(labels).not.toContain('Raporlar');

        // Gruplar tamamen boşaldıysa (tüm öğeleri admin/coordinator-only ise) hiç görünmemeli.
        const groupIds = groups.map((g) => g.id);
        expect(groupIds).not.toContain('organizasyon');
        expect(groupIds).not.toContain('sistem-yonetimi');
        expect(groupIds).not.toContain('raporlama');
    });

    it('"Toplanma Alanları" is visible in the volunteer navigation', () => {
        const labels = flatLabels(getVisibleNavGroups('VOLUNTEER'));
        expect(labels).toContain('Toplanma Alanları');
    });

    it('"Görevlerim" is visible in the volunteer navigation', () => {
        const labels = flatLabels(getVisibleNavGroups('VOLUNTEER'));
        expect(labels).toContain('Görevlerim');
    });

    it('the map item is labelled "Operasyon Haritası", not "Risk Haritası"', () => {
        const mapItem = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.to === '/map');
        expect(mapItem?.label).toBe('Operasyon Haritası');
    });

    it('no standalone "3B Bina Pilotu" nav item — 3B is reached via Operasyon Haritası drill-down', () => {
        const allItems = NAV_GROUPS.flatMap((g) => g.items);
        expect(allItems.find((i) => i.to === '/map-3d')).toBeUndefined();
        expect(allItems.some((i) => /3B/i.test(i.label))).toBe(false);
    });

    it('findActiveGroupId resolves the group that owns the active route', () => {
        expect(findActiveGroupId('/damage-assessments')).toBe('operasyon');
        expect(findActiveGroupId('/admin/users')).toBe('organizasyon');
        expect(findActiveGroupId('/admin/maintenance')).toBe('sistem-yonetimi');
        expect(findActiveGroupId('/emergency/assembly-areas')).toBe('afet-hazirlik');
        expect(findActiveGroupId('/events/some-id')).toBe('operasyon'); // sub-route of /events
        expect(findActiveGroupId('/unknown-route')).toBeUndefined();
    });

    it('coordinators only see groups their role is permitted in', () => {
        const labels = flatLabels(getVisibleNavGroups('DISTRICT_COORDINATOR'));
        expect(labels).toContain('Koordinatör Atamaları');
        expect(labels).not.toContain('Kullanıcı Yönetimi');
        expect(labels).not.toContain('Simülasyonlar');
    });
});
