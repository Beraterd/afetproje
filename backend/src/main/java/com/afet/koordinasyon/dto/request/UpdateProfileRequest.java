package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.BloodType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateProfileRequest {
    @Size(max = 100)
    private String firstName;
    @Size(max = 100)
    private String lastName;
    @Email
    @Size(max = 200)
    private String email;
    @Size(max = 20)
    private String phone;
    private BloodType bloodType;
    private UUID districtId;
    private UUID neighborhoodId;
    private String address;
    @Size(max = 255)
    private String profession;
}
