package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Document;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.DocumentStatus;
import com.afet.koordinasyon.domain.enums.DocumentType;
import com.afet.koordinasyon.domain.enums.NotificationType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DocumentRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.service.email.DocumentApprovedEmailEvent;
import com.afet.koordinasyon.service.email.DocumentRejectedEmailEvent;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${app.storage.local-path:.local-storage}")
    private String localStoragePath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DocumentResponse uploadDocument(UUID userId, DocumentType documentType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Dosya boş olamaz");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessRuleException("Dosya boyutu 10MB'ı geçemez");
        }

        String mime = file.getContentType();
        if (!isAllowedMimeType(mime)) {
            throw new BusinessRuleException("Desteklenmeyen dosya türü. Lütfen PDF, PNG veya JPEG yükleyiniz.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String standardizedFileName = normalizeFileName(user, documentType, originalName);
        String storageKey = userId.toString() + "/" + UUID.randomUUID() + "_" + originalName;
        Path target = Paths.get(localStoragePath, storageKey);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new BusinessRuleException("Dosya kaydedilirken hata oluştu: " + e.getMessage());
        }

        Document doc = Document.builder()
                .user(user)
                .documentType(documentType)
                .storageKey(storageKey)
                .fileName(standardizedFileName)
                .fileSizeBytes(file.getSize())
                .mimeType(mime != null ? mime : "application/octet-stream")
                .build();

        Document saved = documentRepository.save(doc);

        notificationService.createForAdmins(
                NotificationType.DOCUMENT_APPROVAL,
                "Yeni Belge Onay Bekliyor",
                String.format("%s %s yeni bir belge yükledi: %s",
                        user.getFirstName(), user.getLastName(), documentType.name()),
                "Document", saved.getId().toString());

        auditLogService.logUserAction(userId, user.getFirstName() + " " + user.getLastName(),
                user.getRole().name(), AuditActionType.DOCUMENT_UPLOADED, "Document", saved.getId(),
                documentType.name() + " belgesi yüklendi",
                java.util.Map.of("documentType", documentType.name(), "fileName", standardizedFileName));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(UUID userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResponse getDownloadUrl(UUID userId, UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        if (!doc.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Bu belgeye erişim yetkiniz yok");
        }
        String url = baseUrl + "/api/files/download/" + doc.getDownloadToken();
        return DocumentDownloadResponse.builder()
                .presignedUrl(url)
                .expiresAt(OffsetDateTime.now().plusHours(24).toString())
                .build();
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResponse getAdminDocumentViewUrl(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        String url = baseUrl + "/api/files/download/" + doc.getDownloadToken();
        return DocumentDownloadResponse.builder()
                .presignedUrl(url)
                .expiresAt(OffsetDateTime.now().plusHours(24).toString())
                .build();
    }

    public void serveFile(UUID downloadToken, HttpServletResponse response) {
        Document doc = documentRepository.findByDownloadToken(downloadToken)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "downloadToken", downloadToken));

        Path filePath = Paths.get(localStoragePath, doc.getStorageKey());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Dosya", "path", doc.getStorageKey());
        }

        response.setContentType(doc.getMimeType());
        response.setHeader("Content-Disposition", "inline; filename=\"" + doc.getFileName() + "\"");
        response.setContentLengthLong(doc.getFileSizeBytes());

        try {
            Files.copy(filePath, response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessRuleException("Dosya okunurken hata oluştu");
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<PendingDocumentResponse> getPendingDocuments(UUID actorId, UserRole actorRole, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> docs;
        if (actorRole == UserRole.ADMIN) {
            docs = documentRepository.findByStatus(DocumentStatus.PENDING, pageable);
        } else if (actorRole == UserRole.NEIGHBORHOOD_COORDINATOR) {
            User actor = userRepository.findById(actorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", actorId));
            UUID neighborhoodId = actor.getNeighborhood().getId();
            docs = documentRepository.findByStatusAndUserNeighborhoodId(DocumentStatus.PENDING, neighborhoodId, pageable);
        } else {
            User actor = userRepository.findById(actorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", actorId));
            UUID districtId = actor.getDistrict().getId();
            docs = documentRepository.findByStatusAndUserDistrictId(DocumentStatus.PENDING, districtId, pageable);
        }
        return PagedResponse.from(docs.map(this::toPendingResponse));
    }

    @Transactional
    public DocumentResponse approveDocument(UUID reviewerId, UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewerId));
        doc.setStatus(DocumentStatus.APPROVED);
        doc.setReviewedBy(reviewer);
        doc.setReviewedAt(OffsetDateTime.now());
        DocumentResponse response = toResponse(documentRepository.save(doc));
        auditLogService.logUserAction(reviewerId,
                reviewer.getFirstName() + " " + reviewer.getLastName(), reviewer.getRole().name(),
                AuditActionType.DOCUMENT_APPROVED, "Document", documentId,
                doc.getFileName() + " belgesi onaylandı", null);
        eventPublisher.publishEvent(new DocumentApprovedEmailEvent(documentId));
        return response;
    }

    @Transactional
    public DocumentResponse rejectDocument(UUID reviewerId, UUID documentId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Red sebebi belirtilmelidir");
        }
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewerId));
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setRejectionReason(reason);
        doc.setReviewedBy(reviewer);
        doc.setReviewedAt(OffsetDateTime.now());
        DocumentResponse response = toResponse(documentRepository.save(doc));
        auditLogService.logUserAction(reviewerId,
                reviewer.getFirstName() + " " + reviewer.getLastName(), reviewer.getRole().name(),
                AuditActionType.DOCUMENT_REJECTED, "Document", documentId,
                doc.getFileName() + " belgesi reddedildi: " + reason, null);
        eventPublisher.publishEvent(new DocumentRejectedEmailEvent(documentId));
        return response;
    }

    private static final Map<DocumentType, String> DOC_TYPE_SLUG = Map.of(
            DocumentType.SEARCH_RESCUE_CERTIFICATE,     "aramakurtarma",
            DocumentType.PSYCHOSOCIAL_GRADUATION_DOCUMENT, "psikomezuniyet",
            DocumentType.OTHER,                         "diger"
    );

    private String normalizeFileName(User user, DocumentType documentType, String originalFileName) {
        String fullName = (user.getFirstName() + user.getLastName()).toLowerCase();
        // Türkçe karakter dönüşümü
        fullName = fullName
                .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i')
                .replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
                .replace('â', 'a').replace('î', 'i').replace('û', 'u');
        // Unicode normalization (diğer aksan işaretleri)
        fullName = Normalizer.normalize(fullName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // Sadece harf ve rakam bırak
        fullName = fullName.replaceAll("[^a-z0-9]", "");

        String typeSlug = DOC_TYPE_SLUG.getOrDefault(documentType, "belge");

        String ext = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf('.'));
        }

        return fullName + "_" + typeSlug + ext;
    }

    private boolean isAllowedMimeType(String mime) {
        return mime != null && (
                mime.equals("application/pdf") ||
                mime.equals("image/png") ||
                mime.equals("image/jpeg") ||
                mime.equals("image/jpg")
        );
    }

    private DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .documentType(doc.getDocumentType())
                .status(doc.getStatus())
                .fileName(doc.getFileName())
                .fileSizeBytes(doc.getFileSizeBytes())
                .mimeType(doc.getMimeType())
                .rejectionReason(doc.getRejectionReason())
                .reviewedAt(doc.getReviewedAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    private PendingDocumentResponse toPendingResponse(Document doc) {
        User owner = doc.getUser();
        return PendingDocumentResponse.builder()
                .id(doc.getId())
                .documentType(doc.getDocumentType())
                .status(doc.getStatus())
                .fileName(doc.getFileName())
                .owner(DocumentOwnerResponse.builder()
                        .id(owner.getId())
                        .firstName(owner.getFirstName())
                        .lastName(owner.getLastName())
                        .district(owner.getDistrict() != null ? owner.getDistrict().getName() : "")
                        .build())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
