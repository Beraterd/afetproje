package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailRequest {
    @NotBlank
    @Email
    private String newEmail;
    @NotBlank
    private String password;
}
