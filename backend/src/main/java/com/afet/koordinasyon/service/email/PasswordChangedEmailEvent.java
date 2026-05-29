package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record PasswordChangedEmailEvent(UUID userId) {}
