package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OperationsAiRequest(
        @NotBlank(message = "Prompt boş olamaz")
        @Size(max = 2000, message = "Prompt en fazla 2000 karakter olabilir")
        String prompt
) {}
