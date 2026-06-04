package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.WhatsAppProperties;
import com.afet.koordinasyon.domain.entity.EmergencyContact;
import com.afet.koordinasyon.domain.entity.EmergencyStatusMessage;
import com.afet.koordinasyon.domain.entity.EmergencyStatusMessageLog;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.EmergencyStatusTemplateKey;
import com.afet.koordinasyon.domain.enums.NotificationChannel;
import com.afet.koordinasyon.dto.request.SendStatusMessageRequest;
import com.afet.koordinasyon.dto.response.StatusMessageResponse;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.notification.WhatsAppNotificationProvider;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyStatusMessageServiceTest {

    @Mock private EmergencyContactRepository emergencyContactRepository;
    @Mock private EmergencyStatusMessageRepository statusMessageRepository;
    @Mock private EmergencyStatusMessageLogRepository statusMessageLogRepository;
    @Mock private EarthquakeSimulationRepository simulationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailNotificationProvider emailProvider;
    @Mock private WhatsAppNotificationProvider whatsAppProvider;
    @Mock private WhatsAppProperties whatsAppProperties;
    @Mock private EmergencyStatusTemplateResolver templateResolver;

    @InjectMocks private EmergencyStatusMessageService service;

    private User sender;
    private User contactUser;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        UUID senderId = UUID.randomUUID();
        sender = new User();
        sender.setId(senderId);
        sender.setFirstName("Ali");
        sender.setLastName("Veli");
        sender.setEmail("ali@example.com");

        contactUser = new User();
        contactUser.setId(UUID.randomUUID());
        contactUser.setFirstName("Ayşe");
        contactUser.setLastName("Yılmaz");
        contactUser.setEmail("ayse@example.com");
        contactUser.setPhone("05551112233");

        principal = new UserPrincipal(senderId, "Ali", "Veli", "ali@example.com",
                null, null, null, null, true, List.of());
    }

    private void commonStubs(EmergencyStatusTemplateKey key, String text) {
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(sender));
        when(templateResolver.resolve(key)).thenReturn(text);
        when(emergencyContactRepository.findByOwnerIdOrderByCreatedAtAsc(principal.getId()))
                .thenReturn(List.of(EmergencyContact.builder().owner(sender).contact(contactUser).build()));
        when(statusMessageRepository.save(any(EmergencyStatusMessage.class))).thenAnswer(inv -> {
            EmergencyStatusMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(OffsetDateTime.now());
            return m;
        });
        when(whatsAppProperties.isEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("Acil şablon + konum → hem e-posta hem WhatsApp logu ve WhatsApp gövdesinde konum linki")
    void emergencyTemplate_withLocation_sendsBothChannelsAndAppendsLocation() {
        commonStubs(EmergencyStatusTemplateKey.NEED_HELP, "Bulunduğum yerde yardıma ihtiyaç var.");

        SendStatusMessageRequest req = new SendStatusMessageRequest();
        req.setTemplateKey("NEED_HELP");
        req.setLatitude(40.99);
        req.setLongitude(29.02);

        StatusMessageResponse resp = service.sendStatusMessage(req, principal);

        // E-posta gönderildi
        verify(emailProvider).send(eq("ayse@example.com"), anyString(), anyString());

        // WhatsApp gönderildi ve gövdede konum linki var
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppProvider).sendText(eq("05551112233"), bodyCaptor.capture());
        String body = bodyCaptor.getValue();
        assertThat(body).contains("Ali Veli size acil durum mesajı gönderdi:");
        assertThat(body).contains("https://www.google.com/maps?q=40.99,29.02");

        // İki ayrı log (EMAIL + WHATSAPP)
        ArgumentCaptor<EmergencyStatusMessageLog> logCaptor =
                ArgumentCaptor.forClass(EmergencyStatusMessageLog.class);
        verify(statusMessageLogRepository, times(2)).save(logCaptor.capture());
        List<NotificationChannel> channels = logCaptor.getAllValues().stream()
                .map(EmergencyStatusMessageLog::getChannel).toList();
        assertThat(channels).containsExactlyInAnyOrder(NotificationChannel.EMAIL, NotificationChannel.WHATSAPP);

        assertThat(resp.getEmailSentCount()).isEqualTo(1);
        assertThat(resp.getWhatsappSentCount()).isEqualTo(1);
        assertThat(resp.getSentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Güvenli şablon + konum gelse bile WhatsApp gövdesine konum eklenmez")
    void safeTemplate_doesNotAppendLocation() {
        commonStubs(EmergencyStatusTemplateKey.SAFE_AND_WELL, "Şu anda güvendeyim, sağlık durumum iyi.");

        SendStatusMessageRequest req = new SendStatusMessageRequest();
        req.setTemplateKey("SAFE_AND_WELL");
        req.setLatitude(40.99);
        req.setLongitude(29.02);

        service.sendStatusMessage(req, principal);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppProvider).sendText(eq("05551112233"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).doesNotContain("maps?q=");
    }

    @Test
    @DisplayName("WhatsApp devre dışı → yalnızca e-posta logu oluşur")
    void whatsAppDisabled_onlyEmailLog() {
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(sender));
        when(templateResolver.resolve(EmergencyStatusTemplateKey.SAFE_AND_WELL))
                .thenReturn("Şu anda güvendeyim.");
        when(emergencyContactRepository.findByOwnerIdOrderByCreatedAtAsc(principal.getId()))
                .thenReturn(List.of(EmergencyContact.builder().owner(sender).contact(contactUser).build()));
        when(statusMessageRepository.save(any(EmergencyStatusMessage.class))).thenAnswer(inv -> {
            EmergencyStatusMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(OffsetDateTime.now());
            return m;
        });
        when(whatsAppProperties.isEnabled()).thenReturn(false);

        SendStatusMessageRequest req = new SendStatusMessageRequest();
        req.setTemplateKey("SAFE_AND_WELL");

        StatusMessageResponse resp = service.sendStatusMessage(req, principal);

        verify(emailProvider).send(anyString(), anyString(), anyString());
        verify(whatsAppProvider, never()).sendText(anyString(), anyString());
        verify(statusMessageLogRepository, times(1)).save(any());
        assertThat(resp.getWhatsappSentCount()).isZero();
        assertThat(resp.getEmailSentCount()).isEqualTo(1);
    }
}
