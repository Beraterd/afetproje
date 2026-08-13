package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Document;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.DocumentType;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.DocumentRepository;
import com.afet.koordinasyon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Belge indirme yetkilendirmesini kapsar: bir kullanıcının başka bir kullanıcının
 * özel belgesini indirememesi gerekir.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private DocumentService service;

    private User owner;
    private Document document;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "https://afet.example.com");
        owner = new User();
        owner.setId(UUID.randomUUID());
        document = Document.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .documentType(DocumentType.OTHER)
                .storageKey("k")
                .fileName("kimlik.pdf")
                .fileSizeBytes(100)
                .mimeType("application/pdf")
                .downloadToken(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("Sahibi kendi belgesinin indirme linkini alabilir")
    void getDownloadUrl_owner_succeeds() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        var result = service.getDownloadUrl(owner.getId(), document.getId());

        assertThat(result.getPresignedUrl()).contains(document.getDownloadToken().toString());
    }

    @Test
    @DisplayName("Başka bir kullanıcı bu belgeyi indiremez")
    void getDownloadUrl_notOwner_throws() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getDownloadUrl(otherUserId, document.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Bu belgeye erişim yetkiniz yok");
    }
}
