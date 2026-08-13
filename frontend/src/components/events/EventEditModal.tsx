import React from 'react';
import { Button } from '@/components/ui';

export interface EventEditFormState {
    title: string;
    description: string;
    requiredPeople: number;
}

interface EventEditModalProps {
    form: EventEditFormState;
    onChange: (form: EventEditFormState) => void;
    saving: boolean;
    onCancel: () => void;
    onSave: () => void;
}

/** Olay düzenleme modalı — başlık/açıklama/gerekli kişi sayısı. */
export const EventEditModal: React.FC<EventEditModalProps> = ({ form, onChange, saving, onCancel, onSave }) => {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="bg-white rounded-xl shadow-xl p-6 w-full max-w-md space-y-4">
                <h2 className="text-lg font-semibold text-gray-900">Olayı Düzenle</h2>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Başlık</label>
                    <input
                        type="text"
                        value={form.title}
                        onChange={(e) => onChange({ ...form, title: e.target.value })}
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                   focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
                    <textarea
                        value={form.description}
                        onChange={(e) => onChange({ ...form, description: e.target.value })}
                        rows={3}
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                   focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Gerekli Kişi</label>
                    <input
                        type="number"
                        min={1}
                        value={form.requiredPeople}
                        onChange={(e) => onChange({ ...form, requiredPeople: Number(e.target.value) })}
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm
                                   focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="flex justify-end gap-3 pt-2">
                    <Button variant="secondary" onClick={onCancel}>İptal</Button>
                    <Button variant="primary" loading={saving} onClick={onSave}>
                        Kaydet
                    </Button>
                </div>
            </div>
        </div>
    );
};
