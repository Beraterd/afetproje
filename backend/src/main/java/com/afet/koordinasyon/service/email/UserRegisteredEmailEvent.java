package com.afet.koordinasyon.service.email;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEmailEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Instant registeredAt
) {}
