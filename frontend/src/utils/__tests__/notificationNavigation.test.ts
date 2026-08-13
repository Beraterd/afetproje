import { describe, it, expect } from 'vitest';
import { getNotificationTarget } from '@/utils/notificationNavigation';

describe('getNotificationTarget', () => {
    it('routes DAMAGE_REPORT to the damage assessment with a ?selected= deep link', () => {
        const target = getNotificationTarget({ type: 'DAMAGE_REPORT', relatedEntityId: 'dmg-42' });
        expect(target).toBe('/damage-assessments?selected=dmg-42');
    });

    it('routes TEAM_NEED to the specific event detail route', () => {
        const target = getNotificationTarget({ type: 'TEAM_NEED', relatedEntityId: 'evt-7' });
        expect(target).toBe('/events/evt-7');
    });

    it('routes SIMULATION_RESULT to the simulation detail route', () => {
        const target = getNotificationTarget({ type: 'SIMULATION_RESULT', relatedEntityId: 'sim-3' });
        expect(target).toBe('/simulations/sim-3');
    });

    it('falls back to the module list route when relatedEntityId is missing', () => {
        expect(getNotificationTarget({ type: 'DAMAGE_REPORT' })).toBe('/damage-assessments');
        expect(getNotificationTarget({ type: 'TEAM_NEED' })).toBe('/events');
        expect(getNotificationTarget({ type: 'SIMULATION_RESULT' })).toBe('/simulations');
    });

    it('routes DOCUMENT_APPROVAL to the approvals screen regardless of id', () => {
        expect(getNotificationTarget({ type: 'DOCUMENT_APPROVAL', relatedEntityId: 'doc-1' }))
            .toBe('/documents/approvals');
    });

    it('never produces a broken/undefined route for a known notification type', () => {
        const types = [
            'DOCUMENT_APPROVAL', 'RESOURCE_REQUEST', 'TEAM_NEED', 'DAMAGE_REPORT',
            'NEW_EARTHQUAKE', 'SIMULATION_RESULT', 'MESSAGE_DELIVERY_STATUS', 'EMERGENCY_CONTACT_MESSAGE',
        ] as const;
        for (const type of types) {
            const target = getNotificationTarget({ type });
            expect(typeof target).toBe('string');
            expect(target).toMatch(/^\//);
        }
    });
});
