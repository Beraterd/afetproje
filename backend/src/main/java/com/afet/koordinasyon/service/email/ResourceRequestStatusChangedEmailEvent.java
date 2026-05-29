package com.afet.koordinasyon.service.email;

import java.util.UUID;

public record ResourceRequestStatusChangedEmailEvent(UUID resourceRequestId, String newStatus) {}
