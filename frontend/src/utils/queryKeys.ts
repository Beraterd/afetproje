export const queryKeys = {
    auth: {
        me: () => ['auth', 'me'] as const,
    },
    events: {
        all: ['events'] as const,
        list: (filters: any) => ['events', 'list', filters] as const,
        detail: (id: string) => ['events', id] as const,
        volunteers: (id: string) => ['events', id, 'volunteers'] as const,
        participants: (id: string) => ['events', id, 'participants'] as const,
    },
    districts: {
        all: ['districts'] as const,
        detail: (id: string) => ['districts', id] as const,
        neighborhoods: (id: string) => ['districts', id, 'neighborhoods'] as const,
    },
    documents: {
        mine: () => ['documents', 'mine'] as const,
        pending: (filters: any) => ['documents', 'pending', filters] as const,
    },
    simulations: {
        all: ['simulations'] as const,
        detail: (id: string) => ['simulations', id] as const,
        logs: (id: string, status?: string) => ['simulations', id, 'logs', status] as const,
    },
    teams: {
        all: ['teams'] as const,
        members: (id: string) => ['teams', id, 'members'] as const,
    },
    map: {
        districts: () => ['map', 'districts'] as const,
        neighborhoods: (districtId: string) => ['map', 'neighborhoods', districtId] as const,
        damagePoints: (districtId?: string, neighborhoodId?: string) => ['map', 'damage-points', districtId, neighborhoodId] as const,
        districtCenters: () => ['map', 'district-centers'] as const,
        neighborhoodCenters: (districtId?: string) => ['map', 'neighborhood-centers', districtId] as const,
    },
    users: {
        all: ['users'] as const,
        list: (filters: any) => ['users', 'list', filters] as const,
    },
    assemblyAreas: {
        list: (filters: any) => ['assemblyAreas', 'list', filters] as const,
        stats: () => ['assemblyAreas', 'stats'] as const,
        detail: (id: string) => ['assemblyAreas', id] as const,
    },
    coordinationCenters: {
        myStatus: () => ['coordinationCenters', 'myStatus'] as const,
        district: (districtId: string) => ['coordinationCenters', 'district', districtId] as const,
        neighborhood: (neighborhoodId: string) => ['coordinationCenters', 'neighborhood', neighborhoodId] as const,
    },
    earthquakes: {
        all: ['earthquakes'] as const,
        list: (filters: any) => ['earthquakes', 'list', filters] as const,
        latest: () => ['earthquakes', 'latest'] as const,
        detail: (id: string) => ['earthquakes', id] as const,
    },
    notifications: {
        all: ['notifications'] as const,
        list: (filters: any) => ['notifications', 'list', filters] as const,
        unreadCount: () => ['notifications', 'unread-count'] as const,
    },
    audit: {
        list: (filters: any) => ['audit', 'list', filters] as const,
        detail: (id: string) => ['audit', id] as const,
    },
};
