import { useState, useEffect, useCallback } from 'react';
import { Users, Search, X, Plus, Send, CheckCircle, AlertTriangle } from 'lucide-react';
import {
    getMyEmergencyContacts,
    addEmergencyContact,
    removeEmergencyContact,
    searchUsers,
    sendStatusMessage,
    getActiveSimulation,
    getMyStatusMessages,
    getStatusMessageTemplates,
} from '@/api/emergencyContacts.api';
import {
    EmergencyContactResponse,
    UserSearchResponse,
    ActiveSimulationResponse,
    StatusMessageResponse,
    StatusTemplateItem,
} from '@/types';
import { useToast } from '@/components/shared/ToastProvider';

/** İş kuralı: en fazla 3 yakın. Backend EmergencyContactService ile uyumludur. */
const MAX_CONTACTS = 3;

/** Konum istenmesi gereken acil şablonlar (backend EMERGENCY_TEMPLATES ile uyumlu). */
const EMERGENCY_TEMPLATE_KEYS = ['NEED_HELP', 'TRAPPED_UNDER_RUBBLE'];

/** Tarayıcıdan tek seferlik konum almayı dener; başarısız olursa null döner. */
function requestCurrentLocation(): Promise<{ latitude: number; longitude: number; accuracy: number } | null> {
    return new Promise(resolve => {
        if (!('geolocation' in navigator)) {
            resolve(null);
            return;
        }
        navigator.geolocation.getCurrentPosition(
            pos =>
                resolve({
                    latitude: pos.coords.latitude,
                    longitude: pos.coords.longitude,
                    accuracy: pos.coords.accuracy,
                }),
            () => resolve(null),
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    });
}

export function EmergencyContactsPage() {
    const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast();

    // Contacts state
    const [contacts, setContacts] = useState<EmergencyContactResponse[]>([]);
    const [contactsLoading, setContactsLoading] = useState(true);

    // Search state
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<UserSearchResponse[]>([]);
    const [searching, setSearching] = useState(false);

    // Status message state
    const [activeSimulation, setActiveSimulation] = useState<ActiveSimulationResponse | null>(null);
    const [sentMessages, setSentMessages] = useState<StatusMessageResponse[]>([]);
    const [selectedTemplate, setSelectedTemplate] = useState('');
    const [sending, setSending] = useState(false);
    const [templates, setTemplates] = useState<StatusTemplateItem[]>([]);

    const loadContacts = useCallback(async () => {
        try {
            setContactsLoading(true);
            const data = await getMyEmergencyContacts();
            setContacts(data);
        } catch {
            toastError('Yakınlar yüklenemedi');
        } finally {
            setContactsLoading(false);
        }
    }, [toastError]);

    const loadActiveSimulation = useCallback(async () => {
        try {
            const sim = await getActiveSimulation();
            setActiveSimulation(sim);
            if (sim) {
                const msgs = await getMyStatusMessages(sim.id);
                setSentMessages(msgs);
            }
        } catch {
            // ignore
        }
    }, []);

    useEffect(() => {
        loadContacts();
        loadActiveSimulation();
        getStatusMessageTemplates().then(setTemplates).catch(() => {});
    }, [loadContacts, loadActiveSimulation]);

    const handleSearch = useCallback(async (q: string) => {
        setSearchQuery(q);
        if (q.length < 2) {
            setSearchResults([]);
            return;
        }
        setSearching(true);
        try {
            const results = await searchUsers(q);
            setSearchResults(results);
        } catch {
            setSearchResults([]);
        } finally {
            setSearching(false);
        }
    }, []);

    const handleAdd = async (user: UserSearchResponse) => {
        if (contacts.length >= MAX_CONTACTS) {
            toastWarning(`En fazla ${MAX_CONTACTS} yakın ekleyebilirsiniz`);
            return;
        }
        try {
            await addEmergencyContact({ contactUserId: user.id });
            toastSuccess(`${user.firstName} ${user.lastName} yakın listenize eklendi`);
            setSearchQuery('');
            setSearchResults([]);
            loadContacts();
        } catch (err: any) {
            toastError(err?.response?.data?.message || 'Ekleme başarısız');
        }
    };

    const handleRemove = async (contactUserId: string, name: string) => {
        try {
            await removeEmergencyContact(contactUserId);
            toastSuccess(`${name} yakın listenizden çıkarıldı`);
            loadContacts();
        } catch {
            toastError('Çıkarma işlemi başarısız');
        }
    };

    const handleSendMessage = async () => {
        if (!selectedTemplate) {
            toastWarning('Lütfen bir mesaj şablonu seçin');
            return;
        }
        setSending(true);
        try {
            let location: { latitude: number; longitude: number; accuracy: number } | null = null;

            // Yalnızca acil şablonlarda konum iste; güvenli şablonlarda konum istenmez.
            if (EMERGENCY_TEMPLATE_KEYS.includes(selectedTemplate)) {
                location = await requestCurrentLocation();
                if (!location) {
                    toastWarning(
                        'Konum alınamadı. Mesaj konumsuz gönderilecek. ' +
                            'Daha hızlı yardım için konum izni verebilirsiniz.'
                    );
                }
            }

            const result = await sendStatusMessage({
                templateKey: selectedTemplate,
                simulationId: activeSimulation?.id,
                latitude: location?.latitude,
                longitude: location?.longitude,
                locationAccuracy: location?.accuracy,
            });
            toastSuccess(`Mesaj ${result.sentCount} kişiye başarıyla gönderildi`);
            setSelectedTemplate('');
            if (activeSimulation) {
                const msgs = await getMyStatusMessages(activeSimulation.id);
                setSentMessages(msgs);
            }
        } catch (err: any) {
            toastError(err?.response?.data?.message || 'Mesaj gönderilemedi');
        } finally {
            setSending(false);
        }
    };

    const alreadyAdded = (userId: string) => contacts.some(c => c.contactUserId === userId);

    return (
        <div className="max-w-3xl mx-auto space-y-6">
            <div className="flex items-center gap-3">
                <Users className="h-8 w-8 text-blue-600" />
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Yakınlarım</h1>
                    <p className="text-sm text-gray-500">Acil durumlarda bildirim gönderilecek kişiler ({contacts.length}/{MAX_CONTACTS})</p>
                </div>
            </div>

            {/* Active Simulation Banner */}
            {activeSimulation && (
                <div className="bg-red-50 border border-red-300 rounded-lg p-4">
                    <div className="flex items-start gap-3">
                        <span className="text-2xl">⚠️</span>
                        <div className="flex-1">
                            <p className="font-semibold text-red-800">
                                Aktif Deprem Simülasyonu — {activeSimulation.districtName} ({activeSimulation.magnitude} Mw)
                            </p>
                            <p className="text-sm text-red-600 mt-1">
                                Yakınlarınıza durumunuzu bildiren bir mesaj gönderin.
                            </p>
                        </div>
                    </div>

                    {/* Send status message */}
                    <div className="mt-4 space-y-3">
                        <div className="grid grid-cols-1 gap-2">
                            {templates.length === 0 && (
                                <p className="text-xs text-gray-400 py-2">Şablonlar yükleniyor...</p>
                            )}
                            {templates.map(tpl => {
                                const isEmergency = EMERGENCY_TEMPLATE_KEYS.includes(tpl.key);
                                const isSelected = selectedTemplate === tpl.key;
                                const base = 'flex items-center gap-3 p-3.5 rounded-lg border-2 cursor-pointer transition-colors font-medium text-sm';
                                let cls: string;
                                if (isEmergency) {
                                    cls = isSelected
                                        ? 'border-red-600 bg-red-600 text-white'
                                        : 'border-red-300 bg-red-50 text-red-700 hover:bg-red-100';
                                } else {
                                    cls = isSelected
                                        ? 'border-blue-600 bg-blue-50 text-blue-800'
                                        : 'border-gray-200 bg-white text-gray-800 hover:bg-gray-50';
                                }
                                return (
                                    <label key={tpl.key} className={`${base} ${cls}`}>
                                        <input
                                            type="radio"
                                            name="template"
                                            value={tpl.key}
                                            checked={isSelected}
                                            onChange={() => setSelectedTemplate(tpl.key)}
                                            className="flex-shrink-0"
                                        />
                                        <span className="flex items-center gap-2">
                                            {isEmergency && <AlertTriangle className="h-4 w-4 flex-shrink-0" />}
                                            <span>{tpl.label}</span>
                                        </span>
                                    </label>
                                );
                            })}
                        </div>
                        <button
                            onClick={handleSendMessage}
                            disabled={sending || !selectedTemplate || contacts.length === 0}
                            className="w-full flex items-center justify-center gap-2 bg-red-600 hover:bg-red-700 disabled:bg-red-300 text-white font-medium px-4 py-2.5 rounded-lg transition-colors"
                        >
                            <Send className="h-4 w-4" />
                            {sending ? 'Gönderiliyor...' : 'Yakınlarıma Gönder'}
                        </button>
                    </div>

                    {/* Sent messages */}
                    {sentMessages.length > 0 && (
                        <div className="mt-3 pt-3 border-t border-red-200">
                            <p className="text-xs text-red-700 font-medium mb-2">Bu simülasyon için gönderilen mesajlar:</p>
                            <div className="space-y-1">
                                {sentMessages.map(m => (
                                    <div key={m.id} className="flex items-center gap-2 text-xs text-gray-600">
                                        <CheckCircle className="h-3.5 w-3.5 text-green-500 flex-shrink-0" />
                                        <span>"{m.messageText}" — {m.sentCount}/{m.recipientCount} kişiye ulaştı</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* Search to add */}
            <div className="bg-white rounded-xl border border-gray-200 p-5">
                <h2 className="text-base font-semibold text-gray-800 mb-3">Yakın Ekle</h2>
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={e => handleSearch(e.target.value)}
                        placeholder="İsim veya e-posta ile ara..."
                        className="w-full pl-9 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    {searching && (
                        <div className="absolute right-3 top-1/2 -translate-y-1/2">
                            <div className="h-4 w-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                        </div>
                    )}
                </div>

                {searchResults.length > 0 && (
                    <div className="mt-2 border border-gray-200 rounded-lg overflow-hidden">
                        {searchResults.map(user => {
                            const added = alreadyAdded(user.id);
                            return (
                                <div key={user.id} className="flex items-center justify-between px-4 py-3 hover:bg-gray-50 border-b border-gray-100 last:border-0">
                                    <div>
                                        <p className="text-sm font-medium text-gray-800">{user.firstName} {user.lastName}</p>
                                        <p className="text-xs text-gray-500">{user.email}</p>
                                    </div>
                                    <button
                                        onClick={() => handleAdd(user)}
                                        disabled={added || contacts.length >= MAX_CONTACTS}
                                        className={`flex items-center gap-1 text-xs px-3 py-1.5 rounded-lg font-medium transition-colors ${
                                            added
                                                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                : 'bg-blue-600 hover:bg-blue-700 text-white'
                                        }`}
                                    >
                                        {added ? <CheckCircle className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
                                        {added ? 'Eklendi' : 'Ekle'}
                                    </button>
                                </div>
                            );
                        })}
                    </div>
                )}
                {searchQuery.length >= 2 && searchResults.length === 0 && !searching && (
                    <p className="mt-2 text-sm text-gray-500 text-center py-3">Kullanıcı bulunamadı</p>
                )}
            </div>

            {/* Contact list */}
            <div className="bg-white rounded-xl border border-gray-200 p-5">
                <h2 className="text-base font-semibold text-gray-800 mb-3">Yakın Listem</h2>
                {contactsLoading ? (
                    <p className="text-sm text-gray-500 text-center py-6">Yükleniyor...</p>
                ) : contacts.length === 0 ? (
                    <div className="text-center py-8">
                        <Users className="h-12 w-12 text-gray-300 mx-auto mb-2" />
                        <p className="text-sm text-gray-500">Henüz yakın eklenmedi</p>
                        <p className="text-xs text-gray-400 mt-1">Yukarıdan arama yaparak yakın ekleyebilirsiniz</p>
                    </div>
                ) : (
                    <div className="space-y-2">
                        {contacts.map(contact => (
                            <div key={contact.contactUserId} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                                <div className="flex items-center gap-3">
                                    <div className="h-9 w-9 rounded-full bg-blue-100 flex items-center justify-center text-sm font-semibold text-blue-700">
                                        {contact.firstName[0]}{contact.lastName[0]}
                                    </div>
                                    <div>
                                        <p className="text-sm font-medium text-gray-800">{contact.firstName} {contact.lastName}</p>
                                        <p className="text-xs text-gray-500">{contact.email}</p>
                                    </div>
                                </div>
                                <button
                                    onClick={() => handleRemove(contact.contactUserId, `${contact.firstName} ${contact.lastName}`)}
                                    className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                    title="Çıkar"
                                >
                                    <X className="h-4 w-4" />
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
