package com.afet.koordinasyon.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOwnerResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String district;
}
