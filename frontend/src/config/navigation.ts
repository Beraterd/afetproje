import type { LucideIcon } from 'lucide-react';
import {
    LayoutDashboard,
    Map as MapIcon,
    Calendar,
    Users,
    ShieldAlert,
    MapPin,
    UserCheck,
    CheckSquare,
    Briefcase,
    Building2,
    PackageOpen,
    Landmark,
    Wrench,
    Activity,
    ClipboardList,
    FileBarChart2,
    Heart,
} from 'lucide-react';
import { Role } from '@/types';

export interface NavItem {
    label: string;
    to: string;
    icon: LucideIcon;
    roles: Role[];
}

export interface NavGroup {
    id: string;
    label: string;
    items: NavItem[];
    /** false ise grup her zaman açık gösterilir, accordion/chevron yok (ör. "Genel Bakış"). */
    collapsible?: boolean;
}

/**
 * Data-driven sidebar yapısı — grup başına roller `NavItem.roles` üzerinden kontrol edilir,
 * ayrı bir role-kontrol mekanizması icat edilmez (mevcut `Role` tipi kullanılır). Yeni bir
 * menü öğesi eklerken sadece bu diziye eklemek yeterli; SideNav bileşeni grup/rol filtrelemesini
 * otomatik uygular.
 */
export const NAV_GROUPS: NavGroup[] = [
    {
        id: 'genel-bakis',
        label: 'Genel Bakış',
        collapsible: false,
        items: [
            { label: 'Kontrol Paneli', to: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
            { label: 'Görevlerim', to: '/my-tasks', icon: Briefcase, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
        ],
    },
    {
        id: 'operasyon',
        label: 'Operasyon',
        items: [
            { label: 'Operasyon Haritası', to: '/map', icon: MapIcon, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
            { label: 'Olaylar', to: '/events', icon: Calendar, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
            { label: 'Hasar Tespiti', to: '/damage-assessments', icon: Building2, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
            { label: 'Kaynak Talepleri', to: '/resource-requests', icon: PackageOpen, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
            { label: 'Koordinasyon Merkezi', to: '/coordination-center', icon: Landmark, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
        ],
    },
    {
        id: 'afet-hazirlik',
        label: 'Afet & Hazırlık',
        items: [
            { label: 'Depremler', to: '/earthquakes', icon: Activity, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
            { label: 'Toplanma Alanları', to: '/emergency/assembly-areas', icon: MapPin, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
            { label: 'Yakınlarım', to: '/emergency-contacts', icon: Heart, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
        ],
    },
    {
        id: 'raporlama',
        label: 'Raporlama',
        items: [
            { label: 'Raporlar', to: '/reports', icon: FileBarChart2, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
        ],
    },
    {
        id: 'organizasyon',
        label: 'Organizasyon',
        items: [
            { label: 'Koordinatör Atamaları', to: '/admin/coordinators', icon: UserCheck, roles: ['ADMIN', 'DISTRICT_COORDINATOR'] },
            { label: 'Belge Onayları', to: '/documents/approvals', icon: CheckSquare, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
            { label: 'Kullanıcı Yönetimi', to: '/admin/users', icon: Users, roles: ['ADMIN'] },
        ],
    },
    {
        id: 'sistem-yonetimi',
        label: 'Sistem Yönetimi',
        items: [
            { label: 'Simülasyonlar', to: '/simulations', icon: ShieldAlert, roles: ['ADMIN'] },
            { label: 'İşlem Kayıtları', to: '/admin/audit', icon: ClipboardList, roles: ['ADMIN'] },
            { label: 'Sistem Bakımı', to: '/admin/maintenance', icon: Wrench, roles: ['ADMIN'] },
        ],
    },
];

/** Bir kullanıcının rolüne göre görünür gruplar — boş kalan gruplar (hiç izinli öğesi olmayan) elenir. */
export function getVisibleNavGroups(role: Role | undefined): NavGroup[] {
    if (!role) return [];
    return NAV_GROUPS
        .map((group) => ({ ...group, items: group.items.filter((item) => item.roles.includes(role)) }))
        .filter((group) => group.items.length > 0);
}

/** Verilen pathname'in ait olduğu grup id'si — sidebar açılışta/route değişiminde ilgili
 *  grubu otomatik açık tutmak için kullanılır. */
export function findActiveGroupId(pathname: string): string | undefined {
    return NAV_GROUPS.find((group) =>
        group.items.some((item) => pathname === item.to || pathname.startsWith(item.to + '/')),
    )?.id;
}
