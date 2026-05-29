package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record PasswordResetEmailEvent(UUID userId, String resetLink) {}
