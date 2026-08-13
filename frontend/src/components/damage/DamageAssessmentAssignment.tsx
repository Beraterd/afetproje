import React from 'react';
import { X, Trash2 } from 'lucide-react';
import type { DamageAssessmentAssignmentInfo, EligibleAssigneeResponse } from '@/types';

interface DamageAssessmentAssignmentProps {
    assignedList: DamageAssessmentAssignmentInfo[];
    eligibleAssignees: EligibleAssigneeResponse[];
    selectedUserId: string;
    assigning: boolean;
    removingId: string | null;
    onSelectUser: (userId: string) => void;
    onAssign: () => void;
    onRemove: (assignmentId: string) => void;
    onClose: () => void;
}

/** Görevli yönlendirme paneli — mevcut atamalar + yeni görevli ekleme. */
export const DamageAssessmentAssignment: React.FC<DamageAssessmentAssignmentProps> = ({
    assignedList, eligibleAssignees, selectedUserId, assigning, removingId,
    onSelectUser, onAssign, onRemove, onClose,
}) => {
    const availableAssignees = eligibleAssignees.filter(
        (m) => !assignedList.some((a) => a.userId === m.userId),
    );

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl w-full max-w-lg p-6 space-y-5">
                <div className="flex items-start justify-between">
                    <div>
                        <h2 className="text-lg font-semibold text-gray-900">Görevli Yönlendir</h2>
                        <p className="text-xs text-gray-500 mt-0.5">Hasar Tespit Ekibi üyelerinden atama yapabilirsiniz</p>
                    </div>
                    <button onClick={onClose} aria-label="Kapat" className="text-gray-400 hover:text-gray-600">
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div>
                    <h3 className="text-sm font-medium text-gray-700 mb-2">Atanan Görevliler</h3>
                    {assignedList.length === 0 ? (
                        <p className="text-sm text-gray-400 italic">Henüz görevli atanmamış</p>
                    ) : (
                        <ul className="space-y-2">
                            {assignedList.map((a) => (
                                <li key={a.id} className="flex items-center justify-between bg-purple-50 rounded-lg px-3 py-2">
                                    <div>
                                        <span className="text-sm font-medium text-gray-800">{a.firstName} {a.lastName}</span>
                                        <span className="text-xs text-gray-500 ml-2">{a.email}</span>
                                    </div>
                                    <button
                                        onClick={() => onRemove(a.id)}
                                        disabled={removingId === a.id}
                                        className="p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors disabled:opacity-50"
                                        title="Kaldır"
                                        aria-label="Kaldır"
                                    >
                                        <Trash2 className="h-3.5 w-3.5" />
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                <div className="border-t border-gray-100 pt-4">
                    <h3 className="text-sm font-medium text-gray-700 mb-2">Yeni Görevli Ekle</h3>
                    {eligibleAssignees.length === 0 ? (
                        <p className="text-sm text-yellow-600 bg-yellow-50 rounded-lg px-3 py-2">
                            Bu bölgede aktif Hasar Tespit Ekibi görevi alan kişi bulunamadı.
                        </p>
                    ) : (
                        <div className="flex gap-2">
                            <select
                                value={selectedUserId}
                                onChange={(e) => onSelectUser(e.target.value)}
                                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                            >
                                <option value="">Görevli seçin...</option>
                                {availableAssignees.map((m) => (
                                    <option key={m.userId} value={m.userId}>
                                        {m.firstName} {m.lastName} — {m.eventTitle}
                                    </option>
                                ))}
                            </select>
                            <button
                                onClick={onAssign}
                                disabled={!selectedUserId || assigning}
                                className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium disabled:opacity-50 transition-colors"
                            >
                                {assigning ? 'Atanıyor...' : 'Ata'}
                            </button>
                        </div>
                    )}
                </div>

                <div className="flex justify-end pt-1">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                    >
                        Kapat
                    </button>
                </div>
            </div>
        </div>
    );
};
