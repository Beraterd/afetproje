package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.entity.EventVolunteer;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.Team;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.EventVolunteerStatus;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.CreateEventRequest;
import com.afet.koordinasyon.dto.request.UpdateEventRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.service.email.VolunteerJoinedTaskEmailEvent;
import com.afet.koordinasyon.service.email.TaskCompletedEmailEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final DistrictRepository districtRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final DocumentRepository documentRepository;
    private final RiskCalculationService riskCalculationService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagedResponse<EventSummaryResponse> listEvents(
            int page, int size, String status, UUID districtId, UUID neighborhoodId, UUID teamId,
            UserPrincipal principal) {

        EventStatus eventStatus = null;
        if (status != null && !status.isBlank()) {
            try { eventStatus = EventStatus.valueOf(status); } catch (Exception ignored) {}
        }

        // Role-based scoping: DC sees only own district, NC sees only own neighborhood
        if (principal != null) {
            UserRole role = principal.getRole();
            if (role == UserRole.DISTRICT_COORDINATOR && districtId == null && neighborhoodId == null) {
                districtId = districtRepository.findByCoordinatorId(principal.getId())
                        .map(District::getId)
                        .orElse(principal.getDistrictId());
            } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR && neighborhoodId == null) {
                neighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                        .map(Neighborhood::getId)
                        .orElse(principal.getNeighborhoodId());
            }
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Event> spec = EventSpecification.withFilters(eventStatus, districtId, neighborhoodId, teamId);
        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);
        Page<EventSummaryResponse> mapped = eventsPage.map(this::toSummaryResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id, UserPrincipal principal) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
        boolean participating = false;
        if (principal != null) {
            participating = eventVolunteerRepository
                    .findByEventIdAndUserId(id, principal.getId())
                    .map(ev -> ev.getStatus() == EventVolunteerStatus.ASSIGNED)
                    .orElse(false);
        }
        return toFullResponse(event, participating);
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, UserPrincipal principal) {
        Neighborhood neighborhood = neighborhoodRepository.findById(request.getNeighborhoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", request.getNeighborhoodId()));

        // Resolve team: prefer teamName (enum string) over teamId (UUID)
        Team team;
        if (request.getTeamName() != null && !request.getTeamName().isBlank()) {
            try {
                TeamName tn = TeamName.valueOf(request.getTeamName());
                team = teamRepository.findByName(tn)
                        .orElseThrow(() -> new ResourceNotFoundException("Team", "name", request.getTeamName()));
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("Geçersiz ekip türü: " + request.getTeamName());
            }
        } else if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
        } else {
            throw new BusinessRuleException("Ekip seçimi zorunludur (teamName veya teamId gereklidir)");
        }
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        // Rol bazlı yetki kontrolü — dual-source pattern
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new BusinessRuleException("Gönüllüler olay oluşturamaz");
        } else if (role == UserRole.DISTRICT_COORDINATOR) {
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            if (assignedDistrictId == null || !neighborhood.getDistrict().getId().equals(assignedDistrictId)) {
                throw new BusinessRuleException("Sadece kendi ilçeniz için olay oluşturabilirsiniz");
            }
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            UUID assignedNeighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
            if (assignedNeighborhoodId == null || !neighborhood.getId().equals(assignedNeighborhoodId)) {
                throw new BusinessRuleException("Sadece kendi mahalleniz için olay oluşturabilirsiniz");
            }
        }
        // ADMIN: kısıtlama yok

        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : team.getName().getLabel() + " - " + neighborhood.getName();

        Event event = Event.builder()
                .title(title)
                .description(request.getDescription())
                .neighborhood(neighborhood)
                .team(team)
                .createdBy(creator)
                .requiredPeople(request.getRequiredPeople())
                .build();

        Event saved = eventRepository.saveAndFlush(event);

        riskCalculationService.recalculateNeighborhoodRisk(neighborhood.getId());
        riskCalculationService.recalculateDistrictRisk(neighborhood.getDistrict().getId());

        auditLogService.logUserAction(principal, AuditActionType.EVENT_CREATED, "Event", saved.getId(),
                "Olay oluşturuldu: " + saved.getTitle(),
                java.util.Map.of("neighborhood", neighborhood.getName(), "team", team.getName().getLabel()));

        return toFullResponse(saved, false);
    }

    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request, UserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        checkEventManageAccess(event, principal, "güncelleyemezsiniz");

        if (event.getStatus() == EventStatus.CLOSED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException("Kapalı veya tamamlanmış olaylar güncellenemez");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getRequiredPeople() != null) {
            event.setRequiredPeople(request.getRequiredPeople());
        }

        eventRepository.saveAndFlush(event);

        UUID neighborhoodId = event.getNeighborhood().getId();
        UUID districtId     = event.getNeighborhood().getDistrict().getId();
        riskCalculationService.recalculateNeighborhoodRisk(neighborhoodId);
        riskCalculationService.recalculateDistrictRisk(districtId);

        return toFullResponse(event, false);
    }

    @Transactional
    public EventCloseResponse completeEvent(UUID eventId, UserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        checkEventManageAccess(event, principal, "tamamlayamazsınız");

        if (event.getStatus() != EventStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Yalnızca devam eden olaylar tamamlanabilir");
        }

        event.setStatus(EventStatus.COMPLETED);
        event.setClosedAt(OffsetDateTime.now());
        Event saved = eventRepository.saveAndFlush(event);

        // Free all assigned volunteers
        List<EventVolunteer> assigned = eventVolunteerRepository.findByEventIdAndStatus(
                eventId, EventVolunteerStatus.ASSIGNED);
        for (EventVolunteer ev : assigned) {
            ev.setStatus(EventVolunteerStatus.COMPLETED);
        }
        if (!assigned.isEmpty()) {
            eventVolunteerRepository.saveAll(assigned);
        }

        riskCalculationService.recalculateNeighborhoodRisk(event.getNeighborhood().getId());
        riskCalculationService.recalculateDistrictRisk(event.getNeighborhood().getDistrict().getId());

        auditLogService.logUserAction(principal, AuditActionType.EVENT_CLOSED, "Event", eventId,
                "Olay tamamlandı: " + event.getTitle(), null);

        eventPublisher.publishEvent(new TaskCompletedEmailEvent(eventId));

        return EventCloseResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .closedAt(saved.getClosedAt())
                .riskScore(saved.getRiskScore())
                .build();
    }

    @Transactional
    public EventJoinResponse joinEvent(UUID eventId, UserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() == EventStatus.CLOSED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException("Kapalı olaylara katılamazsınız");
        }

        eventVolunteerRepository.findByEventIdAndUserId(eventId, principal.getId()).ifPresent(ev -> {
            if (ev.getStatus() == EventVolunteerStatus.ASSIGNED) {
                throw new BusinessRuleException("Bu olaya zaten katılıyorsunuz");
            }
        });

        long activeCount = eventVolunteerRepository.countByUserIdAndStatus(principal.getId(), EventVolunteerStatus.ASSIGNED);
        if (activeCount > 0) {
            throw new BusinessRuleException(
                    "Zaten aktif bir olayda görevlisiniz. Yeni bir olaya katılmadan önce mevcut olaydan ayrılmalısınız.");
        }

        if (event.getTeam() != null && event.getTeam().getRequiresDocument() != null) {
            var required = event.getTeam().getRequiresDocument();
            var approved = documentRepository.findApprovedByUserAndType(principal.getId(), required);
            if (approved.isEmpty()) {
                throw new BusinessRuleException(
                        "Bu ekibe katılabilmek için onaylanmış belge gereklidir: " + required.name());
            }
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        EventVolunteer ev = EventVolunteer.builder()
                .event(event)
                .user(user)
                .build();

        EventVolunteer saved = eventVolunteerRepository.save(ev);

        auditLogService.logUserAction(principal, AuditActionType.EVENT_JOINED, "Event", eventId,
                principal.getFirstName() + " " + principal.getLastName() + " olaya katıldı: " + event.getTitle(), null);

        eventPublisher.publishEvent(new VolunteerJoinedTaskEmailEvent(eventId, principal.getId()));

        return EventJoinResponse.builder()
                .eventVolunteerId(saved.getId())
                .eventId(event.getId())
                .status(saved.getStatus())
                .joinedAt(saved.getJoinedAt())
                .build();
    }

    @Transactional
    public void leaveEvent(UUID eventId, UserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() == EventStatus.COMPLETED || event.getStatus() == EventStatus.CLOSED) {
            throw new BusinessRuleException("Tamamlanmış veya kapalı olaylardan ayrılamazsınız");
        }

        EventVolunteer ev = eventVolunteerRepository.findByEventIdAndUserId(eventId, principal.getId())
                .orElseThrow(() -> new BusinessRuleException("Bu olaya katılmıyorsunuz"));

        if (ev.getStatus() != EventVolunteerStatus.ASSIGNED) {
            throw new BusinessRuleException("Bu olaydan ayrılamazsınız");
        }

        ev.setStatus(EventVolunteerStatus.WITHDRAWN);
        ev.setLeftAt(OffsetDateTime.now());
        eventVolunteerRepository.save(ev);

        auditLogService.logUserAction(principal, AuditActionType.EVENT_LEFT, "Event", eventId,
                principal.getFirstName() + " " + principal.getLastName() + " olaydan ayrıldı", null);
    }

    @Transactional
    public EventCloseResponse closeEvent(UUID eventId, UserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        checkEventManageAccess(event, principal, "kapatamaz");

        if (event.getStatus() != EventStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Yalnızca devam eden olaylar iptal edilebilir");
        }

        event.setStatus(EventStatus.CLOSED);
        event.setClosedAt(OffsetDateTime.now());
        Event saved = eventRepository.saveAndFlush(event);

        // Free all assigned volunteers so they can join other events
        List<EventVolunteer> assigned = eventVolunteerRepository.findByEventIdAndStatus(
                eventId, EventVolunteerStatus.ASSIGNED);
        for (EventVolunteer ev : assigned) {
            ev.setStatus(EventVolunteerStatus.COMPLETED);
        }
        if (!assigned.isEmpty()) {
            eventVolunteerRepository.saveAll(assigned);
        }

        riskCalculationService.recalculateNeighborhoodRisk(event.getNeighborhood().getId());
        riskCalculationService.recalculateDistrictRisk(event.getNeighborhood().getDistrict().getId());

        auditLogService.logUserAction(principal, AuditActionType.EVENT_CLOSED, "Event", eventId,
                "Olay kapatıldı: " + event.getTitle(), null);

        return EventCloseResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .closedAt(saved.getClosedAt())
                .riskScore(saved.getRiskScore())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<EventVolunteerResponse> getEventVolunteers(UUID eventId, int page, int size) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "joinedAt"));
        Page<EventVolunteer> volunteers = eventVolunteerRepository.findByEventId(eventId, pageable);
        return PagedResponse.from(volunteers.map(ev -> EventVolunteerResponse.builder()
                .userId(ev.getUser().getId())
                .firstName(ev.getUser().getFirstName())
                .lastName(ev.getUser().getLastName())
                .status(ev.getStatus())
                .joinedAt(ev.getJoinedAt())
                .build()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserEventResponse> getMyEvents(UUID userId, int page, int size, String volunteerStatus) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "joinedAt"));
        Page<EventVolunteer> evPage;
        if (volunteerStatus != null && !volunteerStatus.isBlank()) {
            try {
                EventVolunteerStatus statusEnum = EventVolunteerStatus.valueOf(volunteerStatus);
                evPage = eventVolunteerRepository.findByUserIdAndStatus(userId, statusEnum, pageable);
            } catch (IllegalArgumentException ignored) {
                evPage = eventVolunteerRepository.findByUserId(userId, pageable);
            }
        } else {
            evPage = eventVolunteerRepository.findByUserId(userId, pageable);
        }
        return PagedResponse.from(evPage.map(ev -> {
            Event e = ev.getEvent();
            return UserEventResponse.builder()
                    .id(e.getId())
                    .title(e.getTitle())
                    .status(e.getStatus())
                    .team(e.getTeam() != null ? TeamSummaryResponse.builder()
                            .id(e.getTeam().getId())
                            .name(e.getTeam().getName().name())
                            .coefficient(e.getTeam().getCoefficient())
                            .build() : null)
                    .neighborhood(e.getNeighborhood() != null ? NeighborhoodSummaryResponse.builder()
                            .id(e.getNeighborhood().getId())
                            .name(e.getNeighborhood().getName())
                            .districtId(e.getNeighborhood().getDistrict() != null ? e.getNeighborhood().getDistrict().getId() : null)
                            .districtName(e.getNeighborhood().getDistrict() != null ? e.getNeighborhood().getDistrict().getName() : null)
                            .build() : null)
                    .joinedAt(ev.getJoinedAt())
                    .volunteerStatus(ev.getStatus())
                    .build();
        }));
    }

    // Legacy method for backwards compatibility
    @Transactional(readOnly = true)
    public List<EventResponse> listEvents() {
        return eventRepository.findAll().stream()
                .map(e -> toFullResponse(e, false))
                .toList();
    }

    // ── Access check helper ───────────────────────────────────────────────────

    private void checkEventManageAccess(Event event, UserPrincipal principal, String action) {
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new AccessDeniedException("Gönüllüler olayları " + action);
        } else if (role == UserRole.DISTRICT_COORDINATOR) {
            UUID assignedDistrictId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId)
                    .orElse(principal.getDistrictId());
            if (assignedDistrictId == null
                    || event.getNeighborhood() == null
                    || !event.getNeighborhood().getDistrict().getId().equals(assignedDistrictId)) {
                throw new AccessDeniedException("Sadece kendi ilçenizdeki olaylar için bu işlemi yapabilirsiniz");
            }
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            UUID assignedNeighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId)
                    .orElse(principal.getNeighborhoodId());
            if (assignedNeighborhoodId == null
                    || event.getNeighborhood() == null
                    || !event.getNeighborhood().getId().equals(assignedNeighborhoodId)) {
                throw new AccessDeniedException("Sadece kendi mahallenizin olayları için bu işlemi yapabilirsiniz");
            }
        }
        // ADMIN: no restriction
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private EventSummaryResponse toSummaryResponse(Event e) {
        return EventSummaryResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .status(e.getStatus())
                .requiredPeople(e.getRequiredPeople())
                .riskScore(e.getRiskScore())
                .neighborhood(e.getNeighborhood() != null ? NeighborhoodSummaryResponse.builder()
                        .id(e.getNeighborhood().getId())
                        .name(e.getNeighborhood().getName())
                        .districtId(e.getNeighborhood().getDistrict() != null ? e.getNeighborhood().getDistrict().getId() : null)
                        .districtName(e.getNeighborhood().getDistrict() != null ? e.getNeighborhood().getDistrict().getName() : null)
                        .build() : null)
                .team(e.getTeam() != null ? TeamSummaryResponse.builder()
                        .id(e.getTeam().getId())
                        .name(e.getTeam().getName().name())
                        .coefficient(e.getTeam().getCoefficient())
                        .build() : null)
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .startsAt(e.getStartsAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private EventResponse toFullResponse(Event e, boolean isParticipating) {
        int assignedVolunteers = eventVolunteerRepository
                .countByEventIdAndStatus(e.getId(), EventVolunteerStatus.ASSIGNED);
        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .status(e.getStatus())
                .requiredPeople(e.getRequiredPeople())
                .riskScore(e.getRiskScore())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .neighborhood(e.getNeighborhood() != null ? NeighborhoodSummaryResponse.builder()
                        .id(e.getNeighborhood().getId())
                        .name(e.getNeighborhood().getName())
                        .districtId(e.getNeighborhood().getDistrict() != null ? e.getNeighborhood().getDistrict().getId() : null)
                        .districtName(e.getNeighborhood().getDistrict() != null ? e.getNeighborhood().getDistrict().getName() : null)
                        .build() : null)
                .team(e.getTeam() != null ? TeamSummaryResponse.builder()
                        .id(e.getTeam().getId())
                        .name(e.getTeam().getName().name())
                        .coefficient(e.getTeam().getCoefficient())
                        .build() : null)
                .createdBy(e.getCreatedBy() != null ? UserSummaryResponse.builder()
                        .id(e.getCreatedBy().getId())
                        .firstName(e.getCreatedBy().getFirstName())
                        .lastName(e.getCreatedBy().getLastName())
                        .email(e.getCreatedBy().getEmail())
                        .build() : null)
                .assignedVolunteers(assignedVolunteers)
                .isParticipating(isParticipating)
                .startsAt(e.getStartsAt())
                .endsAt(e.getEndsAt())
                .closedAt(e.getClosedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
