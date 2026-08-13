import React from 'react';
import type { DamageAssessmentResponse } from '@/types';

interface StatusOption {
    value: string;
    label: string;
}

interface DamageAssessmentVerificationProps {
    assessment: DamageAssessmentResponse;
    verifyStatus: string;
    verifyNote: string;
    verifying: boolean;
    canApprove: boolean;
    availableStatuses: StatusOption[];
    onStatusChange: (status: string) => void;
    onNoteChange: (note: string) => void;
    onCancel: () => void;
    onSubmit: () => void;
}

/** Doğrulama durumu güncelleme modalı — saha doğrulama / koordinatör onayı akışı. */
export const DamageAssessmentVerification: React.FC<DamageAssessmentVerificationProps> = ({
    assessment, verifyStatus, verifyNote, verifying, canApprove, availableStatuses,
    onStatusChange, onNoteChange, onCancel, onSubmit,
}) => {
    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl w-full max-w-md p-6 space-y-4">
                <h2 className="text-lg font-semibold text-gray-900">Doğrulama Durumunu Güncelle</h2>
                <p className="text-sm text-gray-600">{assessment.address} — {assessment.neighborhoodName}</p>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Durum</label>
                    <select
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                        value={verifyStatus}
                        onChange={(e) => onStatusChange(e.target.value)}
                    >
                        {availableStatuses.map((s) => (
                            <option key={s.value} value={s.value}>{s.label}</option>
                        ))}
                    </select>
                    {verifyStatus === 'KOORDINATOR_ONAYLADI' && !canApprove && (
                        <p className="text-xs text-red-500 mt-1">Bu durum yalnızca ilçe koordinatörü veya admin tarafından atanabilir.</p>
                    )}
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Not (isteğe bağlı)</label>
                    <textarea
                        rows={2}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none"
                        value={verifyNote}
                        onChange={(e) => onNoteChange(e.target.value)}
                    />
                </div>

                <div className="flex gap-3">
                    <button
                        onClick={onCancel}
                        className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                    >
                        İptal
                    </button>
                    <button
                        onClick={onSubmit}
                        disabled={verifying}
                        className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium disabled:opacity-60"
                    >
                        {verifying ? 'Güncelleniyor...' : 'Güncelle'}
                    </button>
                </div>
            </div>
        </div>
    );
};
