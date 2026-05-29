import React, { useState, useRef, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { NotificationBell } from '@/components/shared/NotificationBell';
import { useAuthStore } from '@/store/authStore';
import { cn } from '@/utils/cn';
import {
    LayoutDashboard,
    Map as MapIcon,
    Calendar,
    FileText,
    Users,
    ShieldAlert,
    MapPin,
    UserCheck,
    CheckSquare,
    LogOut,
    Menu,
    User,
    ChevronDown,
    Briefcase,
    Building2,
    PackageOpen,
    Heart,
    Landmark,
    Wrench,
    Activity,
    ClipboardList,
} from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { clearAllOfflineData } from '@/lib/offlineDb';

const navItems = [
    { name: 'Kontrol Paneli', to: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
    { name: 'Risk Haritası', to: '/map', icon: MapIcon, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
    { name: 'Ekip Durumu', to: '/events', icon: Calendar, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
    { name: 'Hasar Tespiti', to: '/damage-assessments', icon: Building2, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
    { name: 'Kaynak Talepleri', to: '/resource-requests', icon: PackageOpen, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
    { name: 'Belge Onayları', to: '/documents/approvals', icon: CheckSquare, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
    { name: 'Koordinasyon Merkezi', to: '/coordination-center', icon: Landmark, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR'] },
    { name: 'Koordinatör Atamaları', to: '/admin/coordinators', icon: UserCheck, roles: ['ADMIN', 'DISTRICT_COORDINATOR'] },
    { name: 'Kullanıcı Yönetimi', to: '/admin/users', icon: Users, roles: ['ADMIN'] },
    { name: 'Audit Log', to: '/admin/audit', icon: ClipboardList, roles: ['ADMIN'] },
    { name: 'Sistem Bakımı', to: '/admin/maintenance', icon: Wrench, roles: ['ADMIN'] },
    { name: 'AFAD Depremleri', to: '/earthquakes', icon: Activity, roles: ['ADMIN', 'DISTRICT_COORDINATOR', 'NEIGHBORHOOD_COORDINATOR', 'VOLUNTEER'] },
    { name: 'Simülasyonlar', to: '/simulations', icon: ShieldAlert, roles: ['ADMIN'] },
];

const roleLabels: Record<string, string> = {
    ADMIN: 'Yönetici',
    DISTRICT_COORDINATOR: 'İlçe Koordinatörü',
    NEIGHBORHOOD_COORDINATOR: 'Mahalle Koordinatörü',
    VOLUNTEER: 'Gönüllü',
};

export const SideNav: React.FC<{ mobileOpen: boolean; setMobileOpen: (v: boolean) => void }> = ({ mobileOpen, setMobileOpen }) => {
    const user = useAuthStore((s) => s.user);
    const filteredNav = navItems.filter((item) => user && item.roles.includes(user.role));

    // Touch handling for swipe-left to close (mirrors memoria)
    const [currentTranslate, setCurrentTranslate] = useState(0);
    const [isDragging, setIsDragging] = useState(false);
    const touchStart = useRef<number | null>(null);

    const onTouchStart = (e: React.TouchEvent) => {
        // Only allow swipe to close if menu is open
        if (!mobileOpen) return;

        setIsDragging(true);
        touchStart.current = e.targetTouches[0].clientX;
        setCurrentTranslate(0);
    };

    const onTouchMove = (e: React.TouchEvent) => {
        if (!isDragging || touchStart.current === null) return;

        const currentX = e.targetTouches[0].clientX;
        const diff = currentX - touchStart.current;

        // Limits:
        // 1. Cannot be positive (moving right would detach from edge)
        // 2. Cannot be less than -width (though visible area limits this naturally)
        if (diff > 0) {
            setCurrentTranslate(0);
        } else {
            setCurrentTranslate(diff);
        }
    };

    const onTouchEnd = () => {
        if (!isDragging) return;

        setIsDragging(false);

        // Width is ~w-72. Use 100px threshold like memoria.
        if (currentTranslate < -100) {
            setMobileOpen(false);
        }

        setCurrentTranslate(0);
        touchStart.current = null;
    };

    const navContent = (
        <div className="flex grow flex-col gap-y-5 overflow-y-auto glass-sidebar px-6 pb-4">
            <div className="flex h-16 shrink-0 items-center">
                <ShieldAlert className="h-8 w-8 text-brand-700 mr-3" />
                <span className="text-xl font-display font-bold text-brand-900 tracking-tight">AfetKoordinasyon</span>
            </div>
            <nav className="flex flex-1 flex-col">
                <ul role="list" className="flex flex-1 flex-col gap-y-7">
                    <li>
                        <ul role="list" className="-mx-2 space-y-1">
                            {filteredNav.map((item) => (
                                <li key={item.name}>
                                    <NavLink
                                        to={item.to}
                                        onClick={() => setMobileOpen(false)}
                                        className={({ isActive }) =>
                                            cn(
                                                isActive
                                                    ? 'bg-brand-500/15 text-brand-800 shadow-sm border border-brand-500/10'
                                                    : 'text-gray-600 hover:text-brand-800 hover:bg-brand-500/5',
                                                'group flex gap-x-3 rounded-xl p-2 text-sm leading-6 font-semibold transition-all duration-200'
                                            )
                                        }
                                    >
                                        <item.icon className="h-6 w-6 shrink-0" aria-hidden="true" />
                                        {item.name}
                                    </NavLink>
                                </li>
                            ))}
                        </ul>
                    </li>
                </ul>
            </nav>
        </div>
    );

    return (
        <>
            {/* Mobile Overlay */}
            {mobileOpen && (
                <div className="fixed inset-0 bg-gray-900/80 z-40 lg:hidden" onClick={() => setMobileOpen(false)} />
            )}

            <aside
                className={cn(
                    'fixed inset-y-0 left-0 z-50 w-72 max-w-xs flex flex-col lg:hidden',
                    isDragging ? '' : 'transition-transform duration-300',
                    mobileOpen ? 'translate-x-0' : '-translate-x-full'
                )}
                style={isDragging ? { transform: `translateX(${currentTranslate}px)` } : undefined}
                onTouchStart={onTouchStart}
                onTouchMove={onTouchMove}
                onTouchEnd={onTouchEnd}
                role="dialog"
                aria-modal="true"
            >
                {navContent}
            </aside>

            <div className="hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-72 lg:flex-col">
                {navContent}
            </div>
        </>
    );
};

export const TopBar: React.FC<{ onMenuClick: () => void }> = ({ onMenuClick }) => {
    const { user, clearAuth } = useAuthStore();
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
                setDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleLogout = async () => {
        clearAuth();
        localStorage.removeItem('afet_token');
        queryClient.clear();
        // Clear offline cache and queue on logout so another user's session is not exposed
        await clearAllOfflineData();
        navigate('/login');
    };

    const go = (path: string) => {
        navigate(path);
        setDropdownOpen(false);
    };

    return (
        <div className="sticky top-0 z-40 flex h-16 shrink-0 items-center gap-x-4 glass-nav px-4 sm:gap-x-6 sm:px-6 lg:px-8">
            <button type="button" className="-m-2.5 p-2.5 text-gray-700 lg:hidden" onClick={onMenuClick}>
                <span className="sr-only">Menüyü Aç</span>
                <Menu className="h-6 w-6" aria-hidden="true" />
            </button>

            <div className="h-6 w-px bg-gray-200 lg:hidden" aria-hidden="true" />

            <div className="flex flex-1 gap-x-4 self-stretch lg:gap-x-6 justify-end items-center">
                <NotificationBell />
                <div className="relative" ref={dropdownRef}>
                    <button
                        onClick={() => setDropdownOpen((v) => !v)}
                        className="flex items-center gap-x-2 rounded-full py-1.5 px-3 text-sm font-semibold text-gray-900 hover:bg-white/50 transition-all duration-200"
                    >
                        <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 shadow-sm">
                            <User className="h-4 w-4 text-white" />
                        </span>
                        <span className="hidden sm:block">
                            {user?.firstName} {user?.lastName}
                        </span>
                        <span className="hidden sm:block text-xs font-normal text-gray-500">
                            {roleLabels[user?.role || ''] || user?.role}
                        </span>
                        <ChevronDown className="h-4 w-4 text-gray-400" />
                    </button>

                    {dropdownOpen && (
                        <div className="absolute right-0 mt-2 w-52 glass-card shadow-lg ring-1 ring-black ring-opacity-5 z-50">
                            <div className="px-4 py-3 border-b border-gray-100">
                                <p className="text-sm font-medium text-gray-900">{user?.firstName} {user?.lastName}</p>
                                <p className="text-xs text-gray-500 mt-0.5">{roleLabels[user?.role || ''] || user?.role}</p>
                            </div>
                            <div className="py-1">
                                <button
                                    onClick={() => go('/profile')}
                                    className="flex w-full items-center gap-x-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    <User className="h-4 w-4 text-gray-400" />
                                    Profilim
                                </button>
                                <button
                                    onClick={() => go('/my-tasks')}
                                    className="flex w-full items-center gap-x-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    <Briefcase className="h-4 w-4 text-gray-400" />
                                    Görevlerim
                                </button>
                                <button
                                    onClick={() => go('/emergency-contacts')}
                                    className="flex w-full items-center gap-x-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    <Heart className="h-4 w-4 text-gray-400" />
                                    Yakınlarım
                                </button>
                                <button
                                    onClick={() => go('/documents')}
                                    className="flex w-full items-center gap-x-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    <FileText className="h-4 w-4 text-gray-400" />
                                    Belgelerim
                                </button>
                                <button
                                    onClick={() => go('/my-records')}
                                    className="flex w-full items-center gap-x-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    <ClipboardList className="h-4 w-4 text-gray-400" />
                                    Kayıtlarım
                                </button>
                                <div className="border-t border-gray-100 mt-1 pt-1">
                                    <button
                                        onClick={handleLogout}
                                        className="flex w-full items-center gap-x-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                                    >
                                        <LogOut className="h-4 w-4" />
                                        Çıkış Yap
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
