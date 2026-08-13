package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.entity.EventVolunteer;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.Team;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.EventInvitationStatus;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.EventVolunteerStatus;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Event yaşam döngüsü geçişleri (join/leave/close/complete) ve yetkisiz/duplike
 * katılım engellerini kapsar — önceden bu servis için hiç test yoktu.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventVolunteerRepository eventVolunteerRepository;
    @Mock private EventAssignmentRepository eventAssignmentRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private RiskCalculationService riskCalculationService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TeamCodeGeneratorService teamCodeGeneratorService;
    @Mock private TeamRecommendationAutoTrigger teamRecommendationAutoTrigger;

    @InjectMocks private EventService service;

    private District district;
    private Neighborhood neighborhood;
    private Team team;
    private Event event;
    private User volunteerUser;

    @BeforeEach
    void setUp() {
        district = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        neighborhood = Neighborhood.builder().id(UUID.randomUUID()).name("Moda").district(district).build();
        team = Team.builder().id(UUID.randomUUID()).name(TeamName.SEARCH_RESCUE).coefficient(BigDecimal.ONE).build();
        event = Event.builder()
                .id(UUID.randomUUID())
                .title("Kadıköy Bina Çökmesi")
                .neighborhood(neighborhood)
                .team(team)
                .status(EventStatus.IN_PROGRESS)
                .requiredPeople(5)
                .build();
        volunteerUser = new User();
        volunteerUser.setId(UUID.randomUUID());
        volunteerUser.setFirstName("Gönüllü");
        volunteerUser.setLastName("Kişi");
    }

    private UserPrincipal principal(UserRole role, UUID id, UUID districtId, UUID neighborhoodId) {
        return new UserPrincipal(id, "Test", "User", "t@x.com",
                null, role, districtId, neighborhoodId, true, false, List.of());
    }

    // ── joinEvent ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gönüllü açık/devam eden bir olaya başarıyla katılabilir")
    void joinEvent_success() {
        UserPrincipal principal = principal(UserRole.VOLUNTEER, volunteerUser.getId(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventVolunteerRepository.findByEventIdAndUserId(event.getId(), principal.getId())).thenReturn(Optional.empty());
        when(eventAssignmentRepository.existsByEventIdAndUserIdAndStatus(event.getId(), principal.getId(), EventInvitationStatus.ACCEPTED))
                .thenReturn(false);
        when(eventVolunteerRepository.countByUserIdAndStatus(principal.getId(), EventVolunteerStatus.ASSIGNED)).thenReturn(0L);
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(volunteerUser));
        when(eventVolunteerRepository.save(any(EventVolunteer.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.joinEvent(event.getId(), principal);

        assertThat(result.getEventId()).isEqualTo(event.getId());
        verify(eventVolunteerRepository).save(any(EventVolunteer.class));
    }

    @Test
    @DisplayName("Kullanıcı zaten bu olaya katılıyorsa tekrar katılamaz (duplicate)")
    void joinEvent_alreadyAssigned_throws() {
        UserPrincipal principal = principal(UserRole.VOLUNTEER, volunteerUser.getId(), null, null);
        EventVolunteer existing = EventVolunteer.builder().event(event).user(volunteerUser)
                .status(EventVolunteerStatus.ASSIGNED).build();
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventVolunteerRepository.findByEventIdAndUserId(event.getId(), principal.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.joinEvent(event.getId(), principal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("zaten katılıyorsunuz");

        verify(eventVolunteerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kullanıcı başka bir olayda aktif görevliyken yeni bir olaya katılamaz")
    void joinEvent_alreadyActiveElsewhere_throws() {
        UserPrincipal principal = principal(UserRole.VOLUNTEER, volunteerUser.getId(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventVolunteerRepository.findByEventIdAndUserId(event.getId(), principal.getId())).thenReturn(Optional.empty());
        when(eventAssignmentRepository.existsByEventIdAndUserIdAndStatus(event.getId(), principal.getId(), EventInvitationStatus.ACCEPTED))
                .thenReturn(false);
        when(eventVolunteerRepository.countByUserIdAndStatus(principal.getId(), EventVolunteerStatus.ASSIGNED)).thenReturn(1L);

        assertThatThrownBy(() -> service.joinEvent(event.getId(), principal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Zaten aktif bir olayda görevlisiniz");

        verify(eventVolunteerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kapalı olaya katılınamaz")
    void joinEvent_closedEvent_throws() {
        event.setStatus(EventStatus.CLOSED);
        UserPrincipal principal = principal(UserRole.VOLUNTEER, volunteerUser.getId(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.joinEvent(event.getId(), principal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Kapalı olaylara katılamazsınız");
    }

    // ── leaveEvent ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Atanmış gönüllü olaydan ayrılabilir")
    void leaveEvent_success() {
        UserPrincipal principal = principal(UserRole.VOLUNTEER, volunteerUser.getId(), null, null);
        EventVolunteer ev = EventVolunteer.builder().event(event).user(volunteerUser)
                .status(EventVolunteerStatus.ASSIGNED).build();
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventVolunteerRepository.findByEventIdAndUserId(event.getId(), principal.getId())).thenReturn(Optional.of(ev));
        when(eventVolunteerRepository.save(any(EventVolunteer.class))).thenAnswer(i -> i.getArgument(0));

        service.leaveEvent(event.getId(), principal);

        assertThat(ev.getStatus()).isEqualTo(EventVolunteerStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("Katılımı olmayan kullanıcı olaydan ayrılamaz")
    void leaveEvent_notJoined_throws() {
        UserPrincipal principal = principal(UserRole.VOLUNTEER, volunteerUser.getId(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventVolunteerRepository.findByEventIdAndUserId(event.getId(), principal.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leaveEvent(event.getId(), principal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Bu olaya katılmıyorsunuz");
    }

    // ── completeEvent / closeEvent — state transition ───────────────────────

    @Test
    @DisplayName("Devam eden olay başarıyla tamamlanabilir (IN_PROGRESS -> COMPLETED)")
    void completeEvent_validTransition_succeeds() {
        UserPrincipal admin = principal(UserRole.ADMIN, UUID.randomUUID(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(eventVolunteerRepository.findByEventIdAndStatus(event.getId(), EventVolunteerStatus.ASSIGNED))
                .thenReturn(List.of());

        var result = service.completeEvent(event.getId(), admin);

        assertThat(result.getStatus()).isEqualTo(EventStatus.COMPLETED);
        assertThat(event.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("Zaten tamamlanmış bir olay tekrar tamamlanamaz (invalid transition)")
    void completeEvent_alreadyCompleted_throws() {
        event.setStatus(EventStatus.COMPLETED);
        UserPrincipal admin = principal(UserRole.ADMIN, UUID.randomUUID(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.completeEvent(event.getId(), admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Yalnızca devam eden olaylar tamamlanabilir");

        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Gönüllü bir olayı kapatamaz (yetkisiz işlem)")
    void closeEvent_volunteer_throwsAccessDenied() {
        UserPrincipal volunteer = principal(UserRole.VOLUNTEER, UUID.randomUUID(), null, null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.closeEvent(event.getId(), volunteer))
                .isInstanceOf(AccessDeniedException.class);

        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("İlçe koordinatörü başka ilçenin olayını kapatamaz")
    void closeEvent_districtCoordinator_wrongDistrict_throwsAccessDenied() {
        UserPrincipal dc = principal(UserRole.DISTRICT_COORDINATOR, UUID.randomUUID(), UUID.randomUUID(), null);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.closeEvent(event.getId(), dc))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sadece kendi ilçenizdeki");

        verify(eventRepository, never()).saveAndFlush(any());
    }
}
