import { useQuery } from '@tanstack/react-query';
import { getActiveEventCount } from '@/api/teamRecommendations.api';
import { getDamageAssessments } from '@/api/damageAssessments.api';
import { getResourceRequests } from '@/api/resourceRequests.api';
import { getPendingDocuments } from '@/api/documents.api';
import { getStockSummary } from '@/api/resourceStocks.api';

export type AttentionSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'INFO';

export interface AttentionItem {
    id: string;
    severity: AttentionSeverity;
    title: string;
    description: string;
    count: number;
    relatedRoute: string;
}

const SEVERITY_ORDER: Record<AttentionSeverity, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, INFO: 3 };

/**
 * Dashboard "Dikkat Gerektirenler" bölümü için — TAMAMEN mevcut endpoint'lerden (yeni bir
 * backend aggregation endpoint'i olmadan) türetilir. Her kaynak ayrı bir `useQuery` olduğundan
 * biri hata verse/yavaş olsa bile diğerleri ve KPI kartları etkilenmez.
 */
export function useAttentionItems() {
    const eventsQuery = useQuery({
        queryKey: ['dashboard', 'attention', 'open-events'],
        queryFn: getActiveEventCount,
        staleTime: 60_000,
    });

    const damageQuery = useQuery({
        queryKey: ['dashboard', 'attention', 'damage-pending-approval'],
        queryFn: async () => {
            const [heavy, collapsed] = await Promise.all([
                getDamageAssessments({ damageLevel: 'HEAVY', verificationStatus: 'SAHADA_DOGRULANDI', size: 1 }),
                getDamageAssessments({ damageLevel: 'COLLAPSED', verificationStatus: 'SAHADA_DOGRULANDI', size: 1 }),
            ]);
            return heavy.totalElements + collapsed.totalElements;
        },
        staleTime: 60_000,
    });

    const resourceQuery = useQuery({
        queryKey: ['dashboard', 'attention', 'resource-requests-priority'],
        queryFn: async () => {
            const [high, critical] = await Promise.all([
                getResourceRequests({ status: 'OPEN', priority: 'HIGH', size: 1 }),
                getResourceRequests({ status: 'OPEN', priority: 'CRITICAL', size: 1 }),
            ]);
            return high.totalElements + critical.totalElements;
        },
        staleTime: 60_000,
    });

    const documentsQuery = useQuery({
        queryKey: ['dashboard', 'attention', 'pending-documents'],
        queryFn: async () => (await getPendingDocuments({ size: 1 })).totalElements,
        staleTime: 60_000,
    });

    const stockQuery = useQuery({
        queryKey: ['dashboard', 'attention', 'stock-summary'],
        queryFn: () => getStockSummary(),
        staleTime: 60_000,
    });

    const items: AttentionItem[] = [];

    if (eventsQuery.data) {
        items.push({
            id: 'team-need',
            severity: 'CRITICAL',
            title: `${eventsQuery.data} açık ekip ihtiyacı karşılanmayı bekliyor`,
            description: 'Devam eden olaylar için ekip/gönüllü ataması gerekiyor.',
            count: eventsQuery.data,
            relatedRoute: '/events',
        });
    }

    if (damageQuery.data) {
        items.push({
            id: 'damage-approval',
            severity: 'HIGH',
            title: `${damageQuery.data} ağır hasar kaydı koordinatör onayı bekliyor`,
            description: 'Sahada doğrulanmış ağır/yıkılmış hasar kayıtları nihai onay bekliyor.',
            count: damageQuery.data,
            relatedRoute: '/damage-assessments?verificationStatus=SAHADA_DOGRULANDI&damageLevel=HEAVY',
        });
    }

    if (resourceQuery.data) {
        items.push({
            id: 'resource-priority',
            severity: 'HIGH',
            title: `${resourceQuery.data} kaynak talebi yüksek öncelikli`,
            description: 'Açık, yüksek veya kritik öncelikli kaynak talepleri var.',
            count: resourceQuery.data,
            relatedRoute: '/resource-requests',
        });
    }

    if (documentsQuery.data) {
        items.push({
            id: 'document-approval',
            severity: 'MEDIUM',
            title: `${documentsQuery.data} belge onay bekliyor`,
            description: 'Gönüllü belgeleri incelemenizi bekliyor.',
            count: documentsQuery.data,
            relatedRoute: '/documents/approvals',
        });
    }

    if (stockQuery.data && (stockQuery.data.criticalCount > 0 || stockQuery.data.outOfStockCount > 0)) {
        const total = stockQuery.data.criticalCount + stockQuery.data.outOfStockCount;
        items.push({
            id: 'critical-stock',
            severity: stockQuery.data.outOfStockCount > 0 ? 'CRITICAL' : 'HIGH',
            title: `${total} kaynak kaleminde kritik veya tükenmiş stok seviyesi var`,
            description: 'Depo stok seviyeleri kritik eşiğin altına düştü.',
            count: total,
            relatedRoute: '/resource-requests?section=stock',
        });
    }

    items.sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity]);

    return {
        items,
        isLoading: eventsQuery.isLoading || damageQuery.isLoading || resourceQuery.isLoading
            || documentsQuery.isLoading || stockQuery.isLoading,
        hasAnyError: eventsQuery.isError && damageQuery.isError && resourceQuery.isError
            && documentsQuery.isError && stockQuery.isError,
    };
}
