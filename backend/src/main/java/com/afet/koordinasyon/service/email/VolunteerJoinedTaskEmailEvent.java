package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record VolunteerJoinedTaskEmailEvent(UUID eventId, UUID userId) {}
