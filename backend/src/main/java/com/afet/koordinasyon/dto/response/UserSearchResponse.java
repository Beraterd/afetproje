package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserSearchResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
}
