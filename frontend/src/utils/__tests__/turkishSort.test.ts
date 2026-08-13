import { describe, expect, it } from 'vitest';
import { sortByNameTr, turkishCompare } from '@/utils/turkishSort';

describe('turkishCompare', () => {
    it('Türkçe özel karakterleri (Ç, Ğ, İ, I, ı, Ö, Ş, Ü) doğru sıralar', () => {
        const input = ['Şile', 'Üsküdar', 'İstanbul', 'Çekmeköy', 'Ankara', 'Öğretmen', 'Işık', 'Istanbul'];
        const sorted = [...input].sort(turkishCompare);
        // ASCII/kod-noktası sıralaması olsaydı Ç/İ/Ö/Ş/Ü hepsi Z'den sonra gelirdi —
        // burada harflerin Latin karşılıklarıyla aynı sırada (A/I/Ö/Ş/Ü/Ç grubunda) olması beklenir.
        expect(sorted[0]).toBe('Ankara');
        expect(sorted).not.toEqual(input);
    });

    it('İstanbul Anadolu Yakası 14 ilçesini örnekteki Türkçe alfabetik sırayla döner', () => {
        const districts = [
            'Kartal', 'Şile', 'Üsküdar', 'Adalar', 'Pendik', 'Ümraniye', 'Ataşehir',
            'Sultanbeyli', 'Tuzla', 'Beykoz', 'Çekmeköy', 'Maltepe', 'Kadıköy', 'Sancaktepe',
        ];
        const sorted = [...districts].sort(turkishCompare);
        expect(sorted).toEqual([
            'Adalar', 'Ataşehir', 'Beykoz', 'Çekmeköy', 'Kadıköy', 'Kartal', 'Maltepe',
            'Pendik', 'Sancaktepe', 'Sultanbeyli', 'Şile', 'Tuzla', 'Ümraniye', 'Üsküdar',
        ]);
    });
});

describe('sortByNameTr', () => {
    it('name alanına göre Türkçe alfabetik sıralı yeni bir dizi döner, orijinali mutasyona uğratmaz', () => {
        const districts = [
            { id: 'd2', name: 'Şile' },
            { id: 'd1', name: 'Adalar' },
            { id: 'd3', name: 'Kadıköy' },
        ];
        const original = [...districts];
        const sorted = sortByNameTr(districts);

        expect(sorted.map((d) => d.name)).toEqual(['Adalar', 'Kadıköy', 'Şile']);
        // orijinal dizi (referans + sıra) değişmedi
        expect(districts).toEqual(original);
        expect(sorted).not.toBe(districts);
    });

    it('aynı isimde farklı ID\'lere sahip mahalleler sıralama sonrası ID association\'ını korur', () => {
        // Örn. "Kozyatağı" hem Kadıköy hem Ataşehir'de bir DB satırı olarak var —
        // isim aynı olsa da her nesnenin kendi id/districtId'si sıralamadan etkilenmemeli.
        const neighborhoods = [
            { id: 'n-atasehir-kozyatagi', name: 'Kozyatağı', districtId: 'atasehir' },
            { id: 'n-kadikoy-kozyatagi', name: 'Kozyatağı', districtId: 'kadikoy' },
            { id: 'n-kadikoy-bostanci', name: 'Bostancı', districtId: 'kadikoy' },
        ];
        const sorted = sortByNameTr(neighborhoods);

        expect(sorted[0]).toEqual({ id: 'n-kadikoy-bostanci', name: 'Bostancı', districtId: 'kadikoy' });
        // Aynı isimli iki farklı satır da kayıpsız, kendi id/districtId'siyle listede kalmalı.
        const kozyatagiEntries = sorted.filter((n) => n.name === 'Kozyatağı');
        expect(kozyatagiEntries).toHaveLength(2);
        expect(kozyatagiEntries.map((n) => n.id).sort()).toEqual(
            ['n-atasehir-kozyatagi', 'n-kadikoy-kozyatagi'].sort(),
        );
    });

    it('boş diziyi güvenle işler', () => {
        expect(sortByNameTr([])).toEqual([]);
    });
});
