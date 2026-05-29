package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.EarthquakeRiskLevel;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.afet.koordinasyon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AfadEarthquakeNotificationServiceTest {

    @Mock private EarthquakeEventRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private EmailNotificationProvider emailNotificationProvider;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AfadEarthquakeNotificationService service;

    private User adminUser;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        // Inject @Value fields via reflection (simulating application.yml defaults)
        ReflectionTestUtils.setField(service, "notificationsEnabled", true);
        ReflectionTestUtils.setField(service, "minMagnitude", 2.5);
        ReflectionTestUtils.setField(service, "recipientRoles",
                List.of("ADMIN", "DISTRICT_COORDINATOR", "NEIGHBORHOOD_COORDINATOR"));
        ReflectionTestUtils.setField(service, "emailTestMode", false);
        ReflectionTestUtils.setField(service, "testRecipient", "");

        eventId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@afet.gov.tr");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setActive(true);
        adminUser.setEmailVerified(true);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private EarthquakeEvent buildEvent(double magnitude, boolean notificationSent) {
        return EarthquakeEvent.builder()
                .id(eventId)
                .externalId("EQ-TEST-" + magnitude)
                .magnitude(magnitude)
                .location("Tuzla")
                .province("İstanbul")
                .district("Tuzla")
                .depth(10.0)
                .eventTime(OffsetDateTime.now())
                .riskLevel(EarthquakeRiskLevel.MEDIUM)
                .notificationSent(notificationSent)
                .build();
    }

    private AfadNewEarthquakeEvent newEvent() {
        return new AfadNewEarthquakeEvent(eventId);
    }

    // ── TEST 1: magnitude 2.4 → mail gönderilmemeli ──────────────────────────

    @Test
    @DisplayName("Test 1 — Magnitude 2.4 → MAIL YOK (threshold 2.5)")
    void magnitude_belowThreshold_noEmailSent() throws Exception {
        EarthquakeEvent eq = buildEvent(2.4, false);
        when(repository.findById(eventId)).thenReturn(Optional.of(eq));

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 1] Magnitude 2.4 < 2.5 → mail gönderilmedi ✓");
        verify(emailNotificationProvider, never()).send(any(), any(), any());
        verify(userRepository, never()).findActiveVerifiedWithRoles(any());
    }

    // ── TEST 2: magnitude 2.5 → mail gönderilmeli ────────────────────────────

    @Test
    @DisplayName("Test 2 — Magnitude 2.5 → MAIL GÖNDER")
    void magnitude_atThreshold_emailSent() throws Exception {
        EarthquakeEvent eq = buildEvent(2.5, false);
        when(repository.findById(eventId)).thenReturn(Optional.of(eq));
        when(userRepository.findActiveVerifiedWithRoles(any())).thenReturn(List.of(adminUser));
        doNothing().when(emailNotificationProvider).send(any(), any(), any());

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 2] Magnitude 2.5 >= 2.5 → mail gönderildi ✓");
        verify(emailNotificationProvider, times(1))
                .send(eq("admin@afet.gov.tr"),
                      eq("Deprem Bildirimi — AFET Koordinasyon Sistemi"),
                      any());
        verify(repository, times(1)).save(argThat(e -> e.isNotificationSent()));
    }

    // ── TEST 3: magnitude 5.1 → mail gönderilmeli (turuncu badge) ────────────

    @Test
    @DisplayName("Test 3 — Magnitude 5.1 → MAIL GÖNDER (turuncu badge)")
    void magnitude_5dot1_emailSentWithOrangeBadge() throws Exception {
        EarthquakeEvent eq = buildEvent(5.1, false);
        when(repository.findById(eventId)).thenReturn(Optional.of(eq));
        when(userRepository.findActiveVerifiedWithRoles(any())).thenReturn(List.of(adminUser));

        // Capture the HTML body to verify badge color
        final String[] capturedBody = {null};
        doAnswer(inv -> { capturedBody[0] = inv.getArgument(2); return null; })
                .when(emailNotificationProvider).send(any(), any(), any());

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 3] Magnitude 5.1 → mail gönderildi ✓");
        System.out.println("[TEST 3] HTML body turuncu badge içeriyor: " +
                (capturedBody[0] != null && capturedBody[0].contains("#f97316") ? "EVET ✓" : "HAYIR ✗"));

        verify(emailNotificationProvider, times(1)).send(any(), any(), any());
        assert capturedBody[0] != null && capturedBody[0].contains("#f97316")
                : "Orange badge color #f97316 expected for magnitude 5.1";
    }

    // ── TEST 4: duplicate guard — aynı event ikinci polling ──────────────────

    @Test
    @DisplayName("Test 4 — Aynı event 2. polling → TEKRAR MAIL GÖNDERME")
    void duplicateEvent_notificationAlreadySent_skipped() throws Exception {
        EarthquakeEvent eq = buildEvent(4.5, true); // notificationSent = true
        when(repository.findById(eventId)).thenReturn(Optional.of(eq));

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 4] isNotificationSent=true → duplicate skipped ✓");
        verify(emailNotificationProvider, never()).send(any(), any(), any());
        verify(userRepository, never()).findActiveVerifiedWithRoles(any());
    }

    // ── TEST 5: notifications disabled ───────────────────────────────────────

    @Test
    @DisplayName("Test 5 — notifications.earthquake.enabled=false → MAIL YOK")
    void notificationsDisabled_noEmailSent() throws Exception {
        ReflectionTestUtils.setField(service, "notificationsEnabled", false);

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 5] notificationsEnabled=false → hiç işlem yapılmadı ✓");
        verify(repository, never()).findById(any());
        verify(emailNotificationProvider, never()).send(any(), any(), any());
    }

    // ── TEST 6: correct subject line ─────────────────────────────────────────

    @Test
    @DisplayName("Test 6 — Mail subject doğru başlık içermeli")
    void emailSubject_correctTitle() throws Exception {
        EarthquakeEvent eq = buildEvent(3.0, false);
        when(repository.findById(eventId)).thenReturn(Optional.of(eq));
        when(userRepository.findActiveVerifiedWithRoles(any())).thenReturn(List.of(adminUser));

        final String[] subject = {null};
        doAnswer(inv -> { subject[0] = inv.getArgument(1); return null; })
                .when(emailNotificationProvider).send(any(), any(), any());

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 6] Mail subject: " + subject[0]);
        assert "Deprem Bildirimi — AFET Koordinasyon Sistemi".equals(subject[0])
                : "Subject mismatch: " + subject[0];
        System.out.println("[TEST 6] Subject doğru ✓");
    }

    // ── TEST 7: magnitude >= 6.0 → kırmızı badge ─────────────────────────────

    @Test
    @DisplayName("Test 7 — Magnitude 6.5 → kırmızı badge (#dc2626)")
    void magnitude_6plus_redBadge() throws Exception {
        EarthquakeEvent eq = buildEvent(6.5, false);
        when(repository.findById(eventId)).thenReturn(Optional.of(eq));
        when(userRepository.findActiveVerifiedWithRoles(any())).thenReturn(List.of(adminUser));

        final String[] capturedBody = {null};
        doAnswer(inv -> { capturedBody[0] = inv.getArgument(2); return null; })
                .when(emailNotificationProvider).send(any(), any(), any());

        service.handleNewEarthquake(newEvent());

        System.out.println("[TEST 7] Magnitude 6.5 → kırmızı badge: " +
                (capturedBody[0] != null && capturedBody[0].contains("#dc2626") ? "EVET ✓" : "HAYIR ✗"));
        assert capturedBody[0] != null && capturedBody[0].contains("#dc2626")
                : "Red badge color #dc2626 expected for magnitude 6.5";
    }
}
