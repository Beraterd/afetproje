package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.DocumentStatus;
import com.afet.koordinasyon.domain.enums.DocumentType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDocumentResponse {
    private UUID id;
    private DocumentType documentType;
    private DocumentStatus status;
    private String fileName;
    private DocumentOwnerResponse owner;
    private OffsetDateTime createdAt;
}
