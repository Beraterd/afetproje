package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record DistrictCoordinatorAssignedEmailEvent(UUID districtId, UUID userId) {}
