import type { NotificationResponse, NotificationType } from '@/types';

/**
 * Bildirim türü → hedef route eşlemesi. Tek merkezi yer — component içinde switch tekrar
 * etmesin diye. `relatedEntityId` yoksa (veya tür bilinmiyorsa) ilgili ana modüle ya da
 * hiçbir yere yönlendirmez (undefined) — kırık route'a gitmek yerine kullanıcı mevcut
 * ekranda kalabilir.
 */
const TYPE_ROUTE_BUILDERS: Record<NotificationType, (relatedEntityId?: string) => string> = {
    DOCUMENT_APPROVAL: () => '/documents/approvals',
    RESOURCE_REQUEST: () => '/resource-requests',
    TEAM_NEED: (id) => (id ? `/events/${id}` : '/events'),
    DAMAGE_REPORT: (id) => (id ? `/damage-assessments?selected=${id}` : '/damage-assessments'),
    NEW_EARTHQUAKE: () => '/earthquakes',
    SIMULATION_RESULT: (id) => (id ? `/simulations/${id}` : '/simulations'),
    MESSAGE_DELIVERY_STATUS: () => '/earthquakes',
    EMERGENCY_CONTACT_MESSAGE: () => '/emergency-contacts',
};

/**
 * Bir bildirim için gidilecek uygulama içi rotayı döner. Eşleşen bir tür/route bulunamazsa
 * `undefined` döner — çağıran taraf bu durumda yalnızca okundu işaretlemeli, kullanıcıyı
 * kırık bir route'a göndermemeli.
 */
export function getNotificationTarget(
    notification: Pick<NotificationResponse, 'type' | 'relatedEntityId'>,
): string | undefined {
    const builder = TYPE_ROUTE_BUILDERS[notification.type];
    if (!builder) return undefined;
    return builder(notification.relatedEntityId || undefined);
}
