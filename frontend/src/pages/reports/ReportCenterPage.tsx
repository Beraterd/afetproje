import React from 'react';
import { Link } from 'react-router-dom';
import {
    CalendarDays, CalendarRange, CalendarClock,
    Activity, PackageOpen, Boxes, AlertTriangle, HeartHandshake, FileBarChart2,
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import type { ReportTypeKey } from '@/types';

interface ReportCard {
    key: ReportTypeKey;
    title: string;
    description: string;
    icon: React.ReactNode;
    color: string;
}

// İlçe Raporu / Mahalle Raporu kartları KALDIRILDI — ilçe/mahalle artık tüm raporlarda filtredir.
const CARDS: ReportCard[] = [
    { key: 'daily',     title: 'Günlük Rapor',          description: 'Seçilen gün için tüm modüllerin operasyonel özeti.', icon: <CalendarDays className="h-6 w-6" />,  color: 'bg-brand-100 text-brand-700' },
    { key: 'weekly',    title: 'Haftalık Rapor',        description: 'Seçilen haftanın kapsamlı toplamı ve dağılımı.',     icon: <CalendarRange className="h-6 w-6" />, color: 'bg-indigo-100 text-indigo-700' },
    { key: 'monthly',   title: 'Aylık Rapor',           description: 'Seçilen ayın kapsamlı performans değerlendirmesi.',   icon: <CalendarClock className="h-6 w-6" />, color: 'bg-violet-100 text-violet-700' },
    { key: 'operation', title: 'Ekip Raporu',            description: 'Ekip durumu, görev performansı ve müdahale süreleri.', icon: <Activity className="h-6 w-6" />,    color: 'bg-emerald-100 text-emerald-700' },
    { key: 'resource',  title: 'Kaynak Talep Raporu',   description: 'Talep durumu, karşılama oranı ve kategori dağılımı.', icon: <PackageOpen className="h-6 w-6" />,    color: 'bg-blue-100 text-blue-700' },
    { key: 'stock',     title: 'Stok Raporu',           description: 'Stok seviyeleri, kritik kalemler ve tüketim.',        icon: <Boxes className="h-6 w-6" />,          color: 'bg-amber-100 text-amber-700' },
    { key: 'damage',    title: 'Hasar Raporu',          description: 'Hasar durumu, risk dağılımı ve mahalle yoğunluğu.',   icon: <AlertTriangle className="h-6 w-6" />, color: 'bg-orange-100 text-orange-700' },
    { key: 'volunteer', title: 'Gönüllü Raporu',        description: 'Gönüllü sayısı, aktiflik ve mahalle dağılımı.',       icon: <HeartHandshake className="h-6 w-6" />, color: 'bg-rose-100 text-rose-700' },
];

export const ReportCenterPage: React.FC = () => {
    const user = useAuthStore((s) => s.user);

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold tracking-tight text-gray-900 flex items-center gap-2">
                    <FileBarChart2 className="h-7 w-7 text-brand-700" />
                    Raporlar
                </h1>
                <p className="mt-1 text-sm text-gray-500">
                    Rapor türünü seçin; ilçe/mahalle ve dönem filtreleriyle oluşturun, PDF/Excel olarak indirin veya yazdırın.
                    {user?.role !== 'ADMIN' && ' Raporlar yetki kapsamınıza göre otomatik filtrelenir.'}
                </p>
            </div>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {CARDS.map((c, i) => (
                    <Link
                        key={c.key}
                        to={`/reports/view?key=${c.key}`}
                        className={`glass-card px-5 py-6 group block animate-stagger-${Math.min(i + 1, 4)}`}
                    >
                        <div className="flex items-start gap-4">
                            <div className={`flex-shrink-0 rounded-xl p-3 ${c.color} shadow-sm transition-transform duration-300 group-hover:scale-110`}>
                                {c.icon}
                            </div>
                            <div>
                                <h3 className="text-base font-semibold text-gray-900 group-hover:text-brand-700 transition-colors">
                                    {c.title}
                                </h3>
                                <p className="mt-1 text-sm text-gray-500">{c.description}</p>
                            </div>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    );
};
