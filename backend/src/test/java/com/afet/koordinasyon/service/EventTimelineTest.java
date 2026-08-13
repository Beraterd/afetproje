package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AuditLog;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.TeamRecommendation;
import com.afet.koordinasyon.domain.enums.RecommendationStatus;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.dto.response.EventTimelineItemResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * EventService.getTimeline'ın yalnızca gerçekten loglanan AuditLog kayıtlarını (Event +
 * ilişkili TeamRecommendation) gösterdiğini, kronolojik sıraladığını ve olay yoksa 404
 * verdiğini doğrular. Sahte/varsayımsal timeline öğesi üretilmediği burada garanti edilir.
 */
@ExtendWith(MockitoExtension.class)
class EventTimelineTest {

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
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private TeamCodeGeneratorService teamCodeGeneratorService;
    @Mock private TeamRecommendationAutoTrigger teamRecommendationAutoTrigger;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private TeamRecommendationRepository teamRecommendationRepository;

    @InjectMocks private EventService service;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
    }

    private AuditLog log(String action, String entityType, UUID entityId, String description, OffsetDateTime createdAt) {
        return AuditLog.builder()
                .id(UUID.randomUUID()).action(action).entityType(entityType).entityId(entityId)
                .description(description).actorName("Ayşe Koordinatör").isSystemAction(false)
                .createdAt(createdAt).build();
    }

    @Test
    @DisplayName("Olay yoksa 404 fırlatır")
    void getTimeline_eventNotFound_throws() {
        when(eventRepository.existsById(eventId)).thenReturn(false);

        assertThatThrownBy(() -> service.getTimeline(eventId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Yalnızca gerçekten loglanmış Event aksiyonları görünür, kronolojik (en yeni önce) sıralanır")
    void getTimeline_returnsOnlyRealEventLogs_newestFirst() {
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(teamRecommendationRepository.findByEventIdOrderByCreatedAtDesc(eventId)).thenReturn(List.of());

        OffsetDateTime t1 = OffsetDateTime.parse("2026-01-01T08:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        AuditLog created = log("EVENT_CREATED", "Event", eventId, "Olay oluşturuldu", t1);
        AuditLog joined = log("EVENT_JOINED", "Event", eventId, "Gönüllü katıldı", t2);

        when(auditLogRepository.findByEntityTypeAndEntityId(eq("Event"), eq(eventId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(created, joined)));

        List<EventTimelineItemResponse> timeline = service.getTimeline(eventId);

        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).getType()).isEqualTo("EVENT_JOINED"); // en yeni önce
        assertThat(timeline.get(0).getTitle()).isEqualTo("Gönüllü katıldı");
        assertThat(timeline.get(1).getType()).isEqualTo("EVENT_CREATED");
    }

    @Test
    @DisplayName("Bu olaya ait TeamRecommendation üzerinden TEAM_ASSIGNED (AI onayı) dahil edilir")
    void getTimeline_includesTeamAssignedViaLinkedRecommendation() {
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("Event"), eq(eventId), any(Pageable.class)))
                .thenReturn(Page.empty());

        District district = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        TeamRecommendation rec = TeamRecommendation.builder()
                .id(UUID.randomUUID()).teamType(TeamName.SEARCH_RESCUE).district(district)
                .requiredTeamSize(2).priority("HIGH").status(RecommendationStatus.APPROVED).build();
        when(teamRecommendationRepository.findByEventIdOrderByCreatedAtDesc(eventId)).thenReturn(List.of(rec));

        AuditLog approved = log("TEAM_ASSIGNED", "TeamRecommendation", rec.getId(),
                "AI ekip önerisi onaylandı ve seçilen kişilere görev daveti gönderildi",
                OffsetDateTime.parse("2026-01-01T12:00:00Z"));
        when(auditLogRepository.findByEntityTypeAndEntityIdIn(eq("TeamRecommendation"), eq(List.of(rec.getId()))))
                .thenReturn(List.of(approved));

        List<EventTimelineItemResponse> timeline = service.getTimeline(eventId);

        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).getType()).isEqualTo("TEAM_ASSIGNED");
        assertThat(timeline.get(0).getDescription()).contains("AI ekip önerisi onaylandı");
        assertThat(timeline.get(0).getActorName()).isEqualTo("Ayşe Koordinatör");
    }

    @Test
    @DisplayName("Actor yoksa (sistem aksiyonu) 'Sistem' fallback'i gösterilir")
    void getTimeline_systemAction_showsSistemFallback() {
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(teamRecommendationRepository.findByEventIdOrderByCreatedAtDesc(eventId)).thenReturn(List.of());

        AuditLog systemLog = AuditLog.builder()
                .id(UUID.randomUUID()).action("EVENT_CREATED").entityType("Event").entityId(eventId)
                .description("Olay otomatik oluşturuldu").actorName(null).isSystemAction(true)
                .createdAt(OffsetDateTime.now()).build();
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("Event"), eq(eventId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(systemLog)));

        List<EventTimelineItemResponse> timeline = service.getTimeline(eventId);

        assertThat(timeline.get(0).getActorName()).isEqualTo("Sistem");
    }
}
