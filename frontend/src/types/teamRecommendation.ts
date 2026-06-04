export interface RecommendedMemberResponse {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    score: number;
    orderId: number;
    completedSimilarTasks: number;
    reasons: string[];
    proximityLevel?: string;
    availability?: string;
    previousSimilarTask?: string;
}

export interface TeamRecommendationResponse {
    id: string;
    eventId?: string;
    teamType: string;
    teamTypeLabel: string;
    districtName: string;
    neighborhoodName?: string;
    requiredTeamSize: number;
    priority: string;
    status: 'DRAFT' | 'APPROVED' | 'REJECTED';
    aiExplanation?: string;
    recommendedPersonnel: RecommendedMemberResponse[];
    requestedByName: string;
    approvedByName?: string;
    approvedAt?: string;
    createdAt: string;
}

export interface TeamRecommendationRequest {
    teamType: string;
    districtId: string;
    neighborhoodId?: string;
    requiredTeamSize: number;
    priority?: string;
}

export interface ApproveRecommendationRequest {
    selectedUserIds: string[];
}

export interface EventAssignmentResponse {
    id: string;
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    status: 'INVITED' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';
    invitedAt: string;
    acceptedAt?: string;
    declinedAt?: string;
    mailSent: boolean;
}

export interface ApproveRecommendationResponse {
    recommendation: TeamRecommendationResponse;
    assignments: EventAssignmentResponse[];
    mailSentCount: number;
    mailFailedCount: number;
    mailNote?: string;
}
