package com.afet.koordinasyon.service;

import com.afet.koordinasyon.config.WhatsAppProperties;
import com.afet.koordinasyon.domain.entity.*;
import com.afet.koordinasyon.domain.enums.EarthquakeSourceType;
import com.afet.koordinasyon.domain.enums.NotificationStatus;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.notification.WhatsAppCloudClient;
import com.afet.koordinasyon.notification.WhatsAppNotificationProvider;
import com.afet.koordinasyon.notification.WhatsAppSendResult;
import com.afet.koordinasyon.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarthquakeAlertNotificationServiceTest {

    @Mock private EarthquakeEventRepository earthquakeEventRepository;
    @Mock private EarthquakeSimulationRepository simulationRepository;
    @Mock private SimulationNotificationLogRepository simulationLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private AssemblyAreaRepository assemblyAreaRepository;
    @Mock private EmailNotificationProvider emailProvider;
    @Mock private ShortLinkService shortLinkService;
    @Mock private NotificationService notificationService;
    @Mock private EmergencyMessageTokenService emergencyMessageTokenService;

    // Gerçek email builder (içerik üretimini doğrulamak için)
    private final EarthquakeAlertEmailBuilder emailBuilder = new EarthquakeAlertEmailBuilder();

    // Testlerde senkron (deterministic) executor — CompletableFuture'lar çağıran thread'de çalışır
    private final TaskExecutor emailTaskExecutor = Runnable::run;

    private User userWithNeighborhood;
    private Neighborhood neighborhood;

    @BeforeEach
    void setUp() {
        neighborhood = Neighborhood.builder().id(UUID.randomUUID()).name("Moda").build();
        userWithNeighborhood = new User();
        userWithNeighborhood.setId(UUID.randomUUID());
        userWithNeighborhood.setFirstName("Ali");
        userWithNeighborhood.setLastName("Veli");
        userWithNeighborhood.setEmail("ali@example.com");
        userWithNeighborhood.setPhone("05551112233");
        userWithNeighborhood.setNeighborhood(neighborhood);
    }

    /** Orchestrator'ı, gerçek email builder + mock WhatsApp provider ile kurar. */
    private EarthquakeAlertNotificationService buildService(WhatsAppNotificationProvider whatsAppProvider,
                                                            WhatsAppProperties props) {
        EarthquakeAlertNotificationService svc = new EarthquakeAlertNotificationService(
                earthquakeEventRepository, simulationRepository, simulationLogRepository,
                userRepository, assemblyAreaRepository, emailProvider, whatsAppProvider,
                props, shortLinkService, emailBuilder, notificationService,
                emergencyMessageTokenService, emailTaskExecutor);
        ReflectionTestUtils.setField(svc, "earthquakeNotificationsEnabled", true);
        ReflectionTestUtils.setField(svc, "minMagnitude", 2.5);
        return svc;
    }

    private EarthquakeEvent realEvent() {
        return EarthquakeEvent.builder()
                .id(UUID.randomUUID())
                .externalId("EQ-1")
                .magnitude(5.2)
                .location("Tuzla")
                .eventTime(OffsetDateTime.now())
                .notificationSent(false)
                .build();
    }

    @Test
    @DisplayName("Gerçek deprem: mail kullanıcının mahalle toplanma alanlarını içerir + işaretlenir")
    void realEarthquake_emailIncludesAssemblyAreas() {
        WhatsAppProperties props = mock(WhatsAppProperties.class);
        when(props.isEnabled()).thenReturn(false); // WhatsApp'ı izole et
        WhatsAppNotificationProvider whatsApp = mock(WhatsAppNotificationProvider.class);
        EarthquakeAlertNotificationService svc = buildService(whatsApp, props);

        EarthquakeEvent eq = realEvent();
        when(earthquakeEventRepository.findById(eq.getId())).thenReturn(Optional.of(eq));
        when(userRepository.findActiveWithNeighborhood()).thenReturn(List.of(userWithNeighborhood));
        AssemblyArea area = AssemblyArea.builder().id(UUID.randomUUID())
                .name("Moda Parkı").address("Moda Cad.").googleMapsUrl("https://maps/abc").build();
        when(assemblyAreaRepository.findActiveApprovedByNeighborhoodId(neighborhood.getId()))
                .thenReturn(List.of(area));

        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromEvent(eq)));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailProvider).send(eq("ali@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("Moda Parkı").contains("https://maps/abc");

        verify(earthquakeEventRepository).save(argThat(EarthquakeEvent::isNotificationSent));
        verify(notificationService).createForAdmins(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Mahalle bilgisi olmayan kullanıcıda mail kırılmaz, boş durum metni yazar")
    void userWithoutNeighborhood_doesNotBreak() {
        WhatsAppProperties props = mock(WhatsAppProperties.class);
        when(props.isEnabled()).thenReturn(false);
        EarthquakeAlertNotificationService svc = buildService(mock(WhatsAppNotificationProvider.class), props);

        User noNb = new User();
        noNb.setId(UUID.randomUUID());
        noNb.setFirstName("Boş");
        noNb.setLastName("Mahalle");
        noNb.setEmail("bos@example.com");

        EarthquakeEvent eq = realEvent();
        when(earthquakeEventRepository.findById(eq.getId())).thenReturn(Optional.of(eq));
        when(userRepository.findActiveWithNeighborhood()).thenReturn(List.of(noNb));

        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromEvent(eq)));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailProvider).send(eq("bos@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains(EarthquakeAlertEmailBuilder.NO_AREAS_MESSAGE);
        verify(assemblyAreaRepository, never()).findActiveApprovedByNeighborhoodId(any());
    }

    @Test
    @DisplayName("Simülasyon: aynı orchestrator üzerinden hem mail hem WhatsApp gönderilir")
    void simulation_sendsEmailAndWhatsApp() {
        WhatsAppProperties props = mock(WhatsAppProperties.class);
        when(props.isEnabled()).thenReturn(true);
        when(props.getEarthquakeTemplateName()).thenReturn("earthquake_alert_tr");
        WhatsAppNotificationProvider whatsApp = mock(WhatsAppNotificationProvider.class);
        EarthquakeAlertNotificationService svc = buildService(whatsApp, props);

        EarthquakeSimulation sim = EarthquakeSimulation.builder()
                .id(UUID.randomUUID())
                .magnitude(new java.math.BigDecimal("6.0"))
                .district(District.builder().id(UUID.randomUUID()).name("Kadıköy").build())
                .triggeredAt(OffsetDateTime.now())
                .build();

        SimulationNotificationLog logEntry = SimulationNotificationLog.builder()
                .id(UUID.randomUUID()).simulation(sim).user(userWithNeighborhood)
                .emailAddress("ali@example.com").status(NotificationStatus.QUEUED).build();

        when(simulationLogRepository.findBySimulationIdAndStatusUnpaged(sim.getId(), NotificationStatus.QUEUED))
                .thenReturn(List.of(logEntry));
        when(assemblyAreaRepository.findActiveApprovedByNeighborhoodId(neighborhood.getId()))
                .thenReturn(List.of());
        when(simulationRepository.findById(sim.getId())).thenReturn(Optional.of(sim));
        when(userRepository.findWhatsappEligibleUsers()).thenReturn(List.of(userWithNeighborhood));
        when(shortLinkService.buildFrontendUrl(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(shortLinkService.createShortLink(any(), any(), any(), any()))
                .thenReturn("http://localhost:8080/s/abc1234");

        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromSimulation(sim)));

        // E-posta gönderildi + log SENT (paralel mimaride saveAll kullanılır)
        verify(emailProvider).send(eq("ali@example.com"), anyString(), anyString());
        verify(simulationLogRepository).saveAll(argThat(logs ->
                ((java.util.List<?>) logs).stream().anyMatch(l ->
                        ((SimulationNotificationLog) l).getStatus() == NotificationStatus.SENT)));
        // WhatsApp parametreli TEMPLATE ile gönderildi (simülasyon da WhatsApp gönderiyor, text DEĞİL)
        verify(whatsApp).sendTemplate(eq("05551112233"), eq("earthquake_alert_tr"),
                argThat(p -> p.contains("http://localhost:8080/s/abc1234")));
        verify(whatsApp, never()).sendText(anyString(), anyString());
        // ShortLink simülasyon için earthquakeEventId=null ile üretildi
        verify(shortLinkService).createShortLink(contains("simulationId="), eq(userWithNeighborhood.getId()),
                isNull(), isNull());
    }

    @Test
    @DisplayName("WhatsApp test mode açıkken simülasyon mesajı da testRecipient'a gider")
    void whatsAppTestMode_simulationGoesToTestRecipient() {
        // Gerçek provider + mock client ile testMode yönlendirmesini doğrula
        WhatsAppProperties props = new WhatsAppProperties();
        props.setEnabled(true);
        props.setTestMode(true);
        props.setTestRecipient("+905550001122");
        props.setEarthquakeTemplateName("hello_world"); // parametresiz template
        WhatsAppCloudClient client = mock(WhatsAppCloudClient.class);
        when(client.sendTemplate(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.ok("wamid.1"));
        WhatsAppNotificationProvider provider = new WhatsAppNotificationProvider(props, client);

        EarthquakeAlertNotificationService svc = buildService(provider, props);

        EarthquakeSimulation sim = EarthquakeSimulation.builder()
                .id(UUID.randomUUID())
                .magnitude(new java.math.BigDecimal("6.0"))
                .district(District.builder().id(UUID.randomUUID()).name("Kadıköy").build())
                .triggeredAt(OffsetDateTime.now())
                .build();

        when(simulationLogRepository.findBySimulationIdAndStatusUnpaged(sim.getId(), NotificationStatus.QUEUED))
                .thenReturn(List.of());
        when(userRepository.findWhatsappEligibleUsers()).thenReturn(List.of(userWithNeighborhood));

        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromSimulation(sim)));

        // Kullanıcının gerçek numarası değil, testRecipient E.164 formatına TEMPLATE gitti;
        // hello_world parametresiz olduğu için kısa link üretilmez.
        verify(client).sendTemplate(eq("+905550001122"), eq("hello_world"), anyString(), anyList());
        verify(client, never()).sendText(anyString(), anyString());
        verify(shortLinkService, never()).createShortLink(any(), any(), any(), any());
    }

    @Test
    @DisplayName("WhatsApp açık ama template adı boş → hata loglanır, hiç mesaj gönderilmez (text fallback yok)")
    void whatsAppEnabledButNoTemplate_skipsWithError() {
        WhatsAppProperties props = mock(WhatsAppProperties.class);
        when(props.isEnabled()).thenReturn(true);
        when(props.getEarthquakeTemplateName()).thenReturn("   "); // boş/whitespace
        WhatsAppNotificationProvider whatsApp = mock(WhatsAppNotificationProvider.class);
        EarthquakeAlertNotificationService svc = buildService(whatsApp, props);

        EarthquakeEvent eq = realEvent();
        when(earthquakeEventRepository.findById(eq.getId())).thenReturn(Optional.of(eq));
        when(userRepository.findActiveWithNeighborhood()).thenReturn(List.of()); // e-postayı izole et
        when(userRepository.findWhatsappEligibleUsers()).thenReturn(List.of(userWithNeighborhood));

        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromEvent(eq)));

        verify(whatsApp, never()).sendTemplate(anyString(), anyString(), anyList());
        verify(whatsApp, never()).sendText(anyString(), anyString());
    }

    @Test
    @DisplayName("Gerçek deprem ve simülasyon aynı handleAlert giriş noktasından geçer")
    void bothSourcesUseSameEntryPoint() {
        WhatsAppProperties props = mock(WhatsAppProperties.class);
        when(props.isEnabled()).thenReturn(false);
        EarthquakeAlertNotificationService svc = buildService(mock(WhatsAppNotificationProvider.class), props);

        // REAL
        EarthquakeEvent eq = realEvent();
        when(earthquakeEventRepository.findById(eq.getId())).thenReturn(Optional.of(eq));
        when(userRepository.findActiveWithNeighborhood()).thenReturn(List.of(userWithNeighborhood));
        when(assemblyAreaRepository.findActiveApprovedByNeighborhoodId(any())).thenReturn(List.of());
        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromEvent(eq)));

        // SIMULATION
        EarthquakeSimulation sim = EarthquakeSimulation.builder()
                .id(UUID.randomUUID()).magnitude(new java.math.BigDecimal("5.0"))
                .district(District.builder().id(UUID.randomUUID()).name("Kadıköy").build())
                .triggeredAt(OffsetDateTime.now()).build();
        when(simulationLogRepository.findBySimulationIdAndStatusUnpaged(sim.getId(), NotificationStatus.QUEUED))
                .thenReturn(List.of());
        svc.handleAlert(new EarthquakeAlertCreatedEvent(EarthquakeAlert.fromSimulation(sim)));

        // REAL email path used findActiveWithNeighborhood; SIM used the simulation logs
        verify(userRepository).findActiveWithNeighborhood();
        verify(simulationLogRepository).findBySimulationIdAndStatusUnpaged(sim.getId(), NotificationStatus.QUEUED);
    }
}
