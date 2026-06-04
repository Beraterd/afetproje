// §9/§10 — Profesyonel yönetim raporu tipleri (backend DailyReportResponse ile birebir).

export interface ReportMeta {
    title: string;
    date: string;
    time: string;
    districtName: string | null;
    neighborhoodName: string | null;
    generatedBy: string;
    role: string;
    scopeLabel: string;
}

export interface ReportResourceStats {
    total: number;
    fulfilled: number;
    pending: number;
    rejected: number;
    fulfillmentRate: number;
}

export interface ReportCategoryCount {
    category: string;
    label: string;
    quantity: number;
}

export interface ReportStockStats {
    totalItems: number;
    criticalItems: number;
    outOfStockItems: number;
    last24hConsumed: number;
    categoryDistribution: ReportCategoryCount[];
}

export interface ReportTaskStats {
    total: number;
    completed: number;
    inProgress: number;
    open: number;
    activeTeams: number;
}

export interface ReportRequestStats {
    total: number;
    open: number;
    closed: number;
    urgent: number;
    critical: number;
}

export interface ReportDamageStats {
    reported: number;
    verified: number;
    pending: number;
    light: number;
    moderate: number;
    heavy: number;
    collapsed: number;
}

export interface ReportAssemblyStats {
    total: number;
    active: number;
    totalCapacity: number;
}

export interface ReportVolunteerStats {
    total: number;
    active: number;
    assigned: number;
}

export interface ReportCommunicationStats {
    sent: number;
    read: number;
    readRate: number;
    last24h: number;
}

export interface ReportExecutiveSummary {
    highlights: string[];
}

export interface ReportRiskFactor {
    label: string;
    points: number;
    detail: string;
}

export type ReportRiskLevel = 'DÜŞÜK' | 'ORTA' | 'YÜKSEK' | 'KRİTİK';

export interface ReportRiskAnalysis {
    score: number;
    level: ReportRiskLevel;
    factors: ReportRiskFactor[];
    warnings: string[];
}

export interface ReportOperationPerformance {
    taskCompletionRate: number;
    requestFulfillmentRate: number;
    distributionPerformance: number;
    avgResponseMinutes: number | null;
    avgResolutionMinutes: number | null;
}

export interface ReportNeighborhoodComparison {
    neighborhoodId: string;
    neighborhoodName: string;
    requestCount: number;
    resourceUsage: number;
    taskCount: number;
    damageCount: number;
    riskScore: number;
}

export interface ReportTrendMetric {
    today: number;
    last7Days: number;
    last30Days: number;
    changeRate: number | null;
}

export interface ReportTrendAnalysis {
    requests: ReportTrendMetric;
    tasks: ReportTrendMetric;
    resourceConsumption: ReportTrendMetric;
    stockChange: ReportTrendMetric;
}

export interface DailyReportResponse {
    meta: ReportMeta;
    resources: ReportResourceStats;
    stock: ReportStockStats;
    tasks: ReportTaskStats;
    requests: ReportRequestStats;
    damage: ReportDamageStats;
    assemblyAreas: ReportAssemblyStats;
    volunteers: ReportVolunteerStats;
    communication: ReportCommunicationStats;
    kpis: string[];
    executiveSummary: ReportExecutiveSummary | null;
    riskAnalysis: ReportRiskAnalysis | null;
    operationPerformance: ReportOperationPerformance | null;
    neighborhoodComparisons: ReportNeighborhoodComparison[] | null;
    trendAnalysis: ReportTrendAnalysis | null;
    aiCommentary: string | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Faz 2 — 8 ayrı rapor türü (backend dto/response/report ile birebir)
// ─────────────────────────────────────────────────────────────────────────────

export type ReportTypeKey =
    | 'daily' | 'weekly' | 'monthly'
    | 'operation' | 'resource' | 'stock' | 'damage' | 'volunteer';

export interface ReportMetaV2 {
    type: string;
    typeLabel: string;
    periodLabel: string;
    from: string;
    to: string;
    districtId: string | null;
    districtName: string | null;
    neighborhoodId: string | null;
    neighborhoodName: string | null;
    scopeLabel: string;
    generatedBy: string;
    role: string;
    generatedAt: string;
}

export interface ReportCategoryCountV2 { code: string; label: string; count: number; }
export interface ReportNeighborhoodMetric { neighborhoodId: string; neighborhoodName: string; value: number; }

export interface TeamModule {
    totalTeams: number; activeTeams: number; availableTeams: number;
    totalTasks: number; completedTasks: number; inProgressTasks: number; openTasks: number;
    requiredPeople: number; shortage: number;
    avgResponseMinutes: number | null; avgCompletionMinutes: number | null;
    teamBreakdown: ReportCategoryCountV2[];
}

export interface DamageModule {
    total: number; newInPeriod: number;
    pendingReview: number; fieldVerified: number; coordinatorApproved: number;
    assignedTeam: number; teamNeeded: number; unassigned: number;
    lightRisk: number; moderateRisk: number; heavyRisk: number; criticalRisk: number;
    avgReviewMinutes: number | null;
    topNeighborhoods: ReportNeighborhoodMetric[];
}

export interface ResourceModule {
    total: number; fulfilled: number; pending: number; rejected: number; partial: number;
    fulfillmentRate: number; avgResolutionMinutes: number | null; criticalPending: number;
    topCategories: ReportCategoryCountV2[];
    topNeighborhoods: ReportNeighborhoodMetric[];
}

export interface StockModule {
    totalItems: number; totalQuantity: number; availableQuantity: number;
    criticalItems: number; outOfStockItems: number;
    periodInflow: number; periodOutflow: number;
    estimatedDaysRemaining: number | null;
    categoryDistribution: ReportCategoryCountV2[];
    neighborhoodDistribution: ReportNeighborhoodMetric[];
}

export interface DocumentModule {
    uploaded: number; approved: number; pending: number; rejected: number;
    typeDistribution: ReportCategoryCountV2[];
}

export interface CoordinatorModule {
    assignmentsInPeriod: number; districtCoordinators: number; neighborhoodCoordinators: number;
    pendingProcesses: number | null;
}

export interface VolunteerModule {
    total: number; newInPeriod: number; approved: number; pending: number;
    active: number; passive: number; assigned: number;
    rejected: number | null; trained: number | null; capacityGap: number | null;
    neighborhoodDistribution: ReportNeighborhoodMetric[];
}

export interface ComprehensiveReportResponse {
    meta: ReportMetaV2;
    team: TeamModule;
    damage: DamageModule;
    resource: ResourceModule;
    stock: StockModule;
    document: DocumentModule;
    coordinator: CoordinatorModule;
    volunteer: VolunteerModule;
    kpis: string[];
    managerSummary: string;
}

export interface OperationReportResponse { meta: ReportMetaV2; team: TeamModule; managerSummary: string; }
export interface ResourceReportResponse { meta: ReportMetaV2; resource: ResourceModule; managerSummary: string; }
export interface StockReportResponse { meta: ReportMetaV2; stock: StockModule; supplyRecommendations: string[]; managerSummary: string; }
export interface DamageReportResponse { meta: ReportMetaV2; damage: DamageModule; managerSummary: string; }
export interface VolunteerReportResponse { meta: ReportMetaV2; volunteer: VolunteerModule; managerSummary: string; }
