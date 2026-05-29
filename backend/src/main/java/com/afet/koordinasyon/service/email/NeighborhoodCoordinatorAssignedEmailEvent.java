package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record NeighborhoodCoordinatorAssignedEmailEvent(UUID neighborhoodId, UUID userId) {}
