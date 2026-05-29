package com.afet.koordinasyon.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDownloadResponse {
    private String presignedUrl;
    private String expiresAt;
}
