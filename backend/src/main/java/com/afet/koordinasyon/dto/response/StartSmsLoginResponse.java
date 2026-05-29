package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StartSmsLoginResponse {
    private UUID challengeId;
    private String maskedPhone;
    private int expiresInSeconds;
}
