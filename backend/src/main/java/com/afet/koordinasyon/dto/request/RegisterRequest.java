package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.BloodType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    private String firstName;
    @NotBlank
    @Size(max = 100)
    private String lastName;
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
    @NotBlank
    @Size(max = 20)
    private String phone;
    @NotNull
    private BloodType bloodType;
    @NotNull
    private UUID districtId;
    @NotNull
    private UUID neighborhoodId;
    @NotBlank
    private String address;
    @Size(max = 255)
    private String profession;
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}
