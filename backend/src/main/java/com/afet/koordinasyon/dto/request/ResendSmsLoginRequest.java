package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ResendSmsLoginRequest {

    @NotNull
    private UUID challengeId;
}
