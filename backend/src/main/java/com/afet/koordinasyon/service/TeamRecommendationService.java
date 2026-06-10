package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.*;
import com.afet.koordinasyon.domain.enums.*;
import com.afet.koordinasyon.dto.request.ApproveRecommendationRequest;
import com.afet.koordinasyon.dto.request.TeamRecommendationRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamRecommendationService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final int SCORE_SAME_NEIGHBORHOOD  = 100;
    private static final int SCORE_SAME_DISTRICT      = 80;
    private static final int SCORE_NEIGHBOR_DISTRICT  = 60;
    private static final int SCORE_ISTANBUL_WIDE      = 40;
    private static final int SCORE_MULTI_COMPLETIONS  = 10;
    private static final int SCORE_RECENT_30_DAY      = 5;

    private static final int TOKEN_EXPIRY_HOURS = 48;

    @Value("${ai.claude-api-key:}")
    private String apiKey;

    @Value("${ai.model:claude-sonnet-4-6}")
    private String model;

    @Value("${app.frontend-url}")
    private String frontendBaseUrl;

    private final TeamRecommendationRepository recommendationRepository;
    private final TeamRecommendationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final EventRepository eventRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final EventAssignmentRepository eventAssignmentRepository;
    private final EmailNotificationService emailNotificationService;
    private final IstanbulDistrictNeighborService neighborService;
    private final ObjectMapper objectMapper;

    // ── Create from event ─────────────────────────────────────────────────────

    @Transactional
    public TeamRecommendationResponse createRecommendationForEvent(UUID eventId, UserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() != EventStatus.IN_PROGRESS && event.getStatus() != EventStatus.OPEN) {
            throw new IllegalStateException("Yalnızca devam eden olaylar için öneri oluşturulabilir.");
        }
        if (event.getTeam() == null) {
            throw new IllegalStateException("Olayın ekibi tanımlanmamış.");
        }

        log.info("[RECOMMENDATION] createRecommendationForEvent eventId={} team={} district={} neighborhood={}",
                eventId,
                event.getTeam().getName(),
                event.getNeighborhood() != null && event.getNeighborhood().getDistrict() != null
                        ? event.getNeighborhood().getDistrict().getName() : "null",
                event.getNeighborhood() != null ? event.getNeighborhood().getName() : "null");

        TeamRecommendationRequest req = new TeamRecommendationRequest(
                event.getTeam().getName().name(),
                event.getNeighborhood().getDistrict().getId(),
                event.getNeighborhood().getId(),
                event.getRequiredPeople(),
                "HIGH"
        );
        User requestedBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        UUID enforceDistrictId = principal.getRole() == UserRole.DISTRICT_COORDINATOR
                ? principal.getDistrictId() : null;
        return createRecommendationInternal(req, requestedBy, enforceDistrictId, event);
    }

    @Transactional
    public TeamRecommendationResponse createRecommendation(TeamRecommendationRequest req, UserPrincipal principal) {
        User requestedBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        UUID enforceDistrictId = principal.getRole() == UserRole.DISTRICT_COORDINATOR
                ? principal.getDistrictId() : null;
        return createRecommendationInternal(req, requestedBy, enforceDistrictId, null);
    }

    /**
     * §3 — Olay oluşturulduğunda arka planda otomatik ekip önerisi üretir.
     * Sistem tetiklemelidir (afterCommit + @Async wrapper); rol kısıtı uygulanmaz çünkü
     * olay zaten oluşturulurken yetki doğrulanmıştır. İdempotent: bu olay için zaten bir
     * öneri varsa yeni öneri üretmez. Hata olursa olay/akış bozulmaz (çağıran tarafça yutulur).
     */
    @Transactional
    public void autoGenerateForEvent(UUID eventId, UUID requestedByUserId) {
        if (recommendationRepository.findTopByEventIdOrderByCreatedAtDesc(eventId).isPresent()) {
            log.info("[RECOMMENDATION] auto-skip: event {} already has a recommendation", eventId);
            return;
        }
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            log.warn("[RECOMMENDATION] auto-skip: event {} not found", eventId);
            return;
        }
        if (event.getStatus() != EventStatus.IN_PROGRESS && event.getStatus() != EventStatus.OPEN) return;
        if (event.getTeam() == null || event.getNeighborhood() == null
                || event.getNeighborhood().getDistrict() == null) return;

        User requestedBy = userRepository.findById(requestedByUserId).orElse(null);
        if (requestedBy == null) {
            log.warn("[RECOMMENDATION] auto-skip: requester {} not found", requestedByUserId);
            return;
        }

        TeamRecommendationRequest req = new TeamRecommendationRequest(
                event.getTeam().getName().name(),
                event.getNeighborhood().getDistrict().getId(),
                event.getNeighborhood().getId(),
                event.getRequiredPeople(),
                "HIGH"
        );
        createRecommendationInternal(req, requestedBy, null, event);
        log.info("[RECOMMENDATION] auto-generated recommendation for event {}", eventId);
    }

    private TeamRecommendationResponse createRecommendationInternal(TeamRecommendationRequest req,
                                                                      User requestedBy,
                                                                      UUID enforceDistrictId,
                                                                      Event linkedEvent) {
        TeamName teamType = parseTeamType(req.teamType());

        District district = districtRepository.findById(req.districtId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", req.districtId()));

        Neighborhood neighborhood = req.neighborhoodId() != null
                ? neighborhoodRepository.findById(req.neighborhoodId()).orElse(null)
                : null;

        if (enforceDistrictId != null && !district.getId().equals(enforceDistrictId)) {
            throw new AccessDeniedException("İlçe koordinatörü yalnızca kendi ilçesi için öneri oluşturabilir.");
        }

        Set<String> neighborNames = neighborService.getNeighbors(district.getName());
        UUID linkedEventId = linkedEvent != null ? linkedEvent.getId() : null;

        log.info("[RECOMMENDATION] teamType={} district={} neighborhood={} requiredPeople={} neighborNames={}",
                teamType, district.getName(),
                neighborhood != null ? neighborhood.getName() : "null",
                req.requiredTeamSize(), neighborNames);

        List<ScoredCandidate> candidates = scoreEligibleCandidates(
                teamType, district, neighborhood, neighborNames, linkedEventId);

        log.info("[RECOMMENDATION] Total eligible candidates after all filters: {}", candidates.size());

        int topN = Math.min(req.requiredTeamSize(), candidates.size());
        List<ScoredCandidate> top = candidates.subList(0, topN);

        String aiExplanation = buildAiExplanation(teamType, district, neighborhood, req.requiredTeamSize(), top);

        TeamRecommendation rec = TeamRecommendation.builder()
                .teamType(teamType).district(district).neighborhood(neighborhood)
                .requiredTeamSize(req.requiredTeamSize())
                .priority(req.priority() != null ? req.priority() : "MEDIUM")
                .status(RecommendationStatus.DRAFT)
                .aiExplanation(aiExplanation).requestedBy(requestedBy).event(linkedEvent)
                .build();
        rec = recommendationRepository.save(rec);

        List<TeamRecommendationMember> members = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            ScoredCandidate sc = top.get(i);
            members.add(TeamRecommendationMember.builder()
                    .recommendation(rec).user(sc.user()).score(sc.score()).orderId(i + 1)
                    .completedSimilarTasks(sc.completedSimilarTasks())
                    .reasons(String.join("|", sc.reasons()))
                    .proximityLevel(sc.proximityLevel()).availability("AVAILABLE")
                    .previousSimilarTask(sc.previousSimilarTask()).build());
        }
        memberRepository.saveAll(members);
        rec.setMembers(members);
        return toResponse(rec, members);
    }

    // ── SCORING ENGINE ────────────────────────────────────────────────────────

    private List<ScoredCandidate> scoreEligibleCandidates(TeamName teamType, District district,
                                                           Neighborhood neighborhood,
                                                           Set<String> neighborNames, UUID linkedEventId) {
        List<User> volunteers = userRepository.findActiveVolunteersWithEmail(UserRole.VOLUNTEER);
        log.info("[SCORING] STEP-1 ROLE FILTER: Found {} active VOLUNTEER users", volunteers.size());

        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        List<ScoredCandidate> result = new ArrayList<>();

        for (User u : volunteers) {
            String uid = u.getEmail();

            log.debug("[SCORING] USER={} role={} active={}", uid, u.getRole(), u.isActive());

            // STEP 4: completed same task history
            List<EventVolunteer> evHistory = eventVolunteerRepository.findByUserIdAll(u.getId());
            List<EventAssignment> eaHistory = eventAssignmentRepository
                    .findByUserIdAndStatusWithEvent(u.getId(), EventInvitationStatus.ACCEPTED);

            log.debug("[SCORING] USER={} evHistory.size={} eaHistory.size={}", uid, evHistory.size(), eaHistory.size());

            // Log each ev record for debug
            for (EventVolunteer ev : evHistory) {
                if (ev.getEvent() == null) {
                    log.debug("[SCORING]   EV record: event=NULL");
                    continue;
                }
                TeamName evTeam = ev.getEvent().getTeam() != null ? ev.getEvent().getTeam().getName() : null;
                log.debug("[SCORING]   EV record: eventId={} eventStatus={} teamName={} evStatus={} evTeamMatchTarget={}",
                        ev.getEvent().getId(), ev.getEvent().getStatus(),
                        evTeam, ev.getStatus(),
                        teamType.equals(evTeam));
            }

            List<CompletedTask> completedSameType = getCompletedSameTypeTasks(evHistory, eaHistory, teamType);
            log.info("[SCORING] USER={} STEP-4 completedSameType.size={}", uid, completedSameType.size());

            if (completedSameType.isEmpty()) {
                log.info("[SCORING] USER={} EXCLUDED: NO_COMPLETED_SAME_TASK_HISTORY", uid);
                continue;
            }

            // STEP 5: currently active on another event?
            boolean currentlyActive = isCurrentlyActiveOnAnotherEvent(evHistory, linkedEventId);
            log.info("[SCORING] USER={} STEP-5 currentlyActive={}", uid, currentlyActive);
            if (currentlyActive) {
                log.info("[SCORING] USER={} EXCLUDED: CURRENTLY_ACTIVE_ON_ANOTHER_EVENT", uid);
                continue;
            }

            // STEP 6: already in this event?
            if (linkedEventId != null) {
                boolean alreadyJoined = eventVolunteerRepository
                        .existsByEventIdAndUserIdAndStatus(linkedEventId, u.getId(), EventVolunteerStatus.ASSIGNED);
                boolean alreadyInvited = eventAssignmentRepository.existsByEventIdAndUserId(linkedEventId, u.getId());
                log.info("[SCORING] USER={} STEP-6 alreadyJoined={} alreadyInvited={}", uid, alreadyJoined, alreadyInvited);
                if (alreadyJoined || alreadyInvited) {
                    log.info("[SCORING] USER={} EXCLUDED: ALREADY_IN_THIS_EVENT", uid);
                    continue;
                }
            }

            // STEP 7: proximity
            ProximityResult prox = computeProximity(completedSameType, district, neighborhood, neighborNames);
            log.info("[SCORING] USER={} STEP-7 proximity={} points={}", uid, prox.level(), prox.points());

            // STEP 8: score
            int score = prox.points();
            if (completedSameType.size() > 1) score += SCORE_MULTI_COMPLETIONS;
            boolean recentCompletion = completedSameType.stream()
                    .anyMatch(t -> t.completedAt() != null && t.completedAt().isAfter(thirtyDaysAgo));
            if (recentCompletion) score += SCORE_RECENT_30_DAY;

            List<String> reasons = new ArrayList<>();
            reasons.add(prox.reason());
            if (completedSameType.size() > 1) reasons.add(completedSameType.size() + " tamamlanmış görev");
            if (recentCompletion) reasons.add("Son 30 günde tamamlandı");

            log.info("[SCORING] USER={} STEP-9 INCLUDED score={}", uid, score);
            result.add(new ScoredCandidate(u, score, completedSameType.size(),
                    reasons, prox.level(), "AVAILABLE", prox.sampleTask()));
        }

        result.sort(Comparator.comparingInt(ScoredCandidate::score).reversed()
                .thenComparing(c -> c.user().getFirstName()));

        // Summary
        long sameNeigh = result.stream().filter(c -> "SAME_NEIGHBORHOOD".equals(c.proximityLevel())).count();
        long sameDist  = result.stream().filter(c -> "SAME_DISTRICT".equals(c.proximityLevel())).count();
        long neighbor  = result.stream().filter(c -> "NEIGHBOR_DISTRICT".equals(c.proximityLevel())).count();
        long istanbul  = result.stream().filter(c -> "ISTANBUL_WIDE".equals(c.proximityLevel())).count();
        log.info("[SCORING] SUMMARY sameNeighborhood={} sameDistrict={} neighborDistrict={} istanbulWide={} TOTAL={}",
                sameNeigh, sameDist, neighbor, istanbul, result.size());

        return result;
    }

    /**
     * Hard filter: başarıyla tamamlanmış aynı görev tipi.
     *
     * Eligible:
     *  EventVolunteer: event.status=COMPLETED AND ev.status IN (ASSIGNED, COMPLETED)
     *  EventAssignment: event.status=COMPLETED AND ea.status=ACCEPTED
     *
     * NOT eligible: WITHDRAWN, CANCELLED, event NOT COMPLETED
     */
    private List<CompletedTask> getCompletedSameTypeTasks(List<EventVolunteer> evHistory,
                                                           List<EventAssignment> eaHistory,
                                                           TeamName targetType) {
        List<CompletedTask> result = new ArrayList<>();

        for (EventVolunteer ev : evHistory) {
            if (ev.getEvent() == null) continue;

            EventStatus eventStatus = ev.getEvent().getStatus();
            if (eventStatus == null || eventStatus != EventStatus.COMPLETED) continue;

            if (ev.getEvent().getTeam() == null) continue;
            TeamName evTeam = ev.getEvent().getTeam().getName();
            if (evTeam == null) continue;

            // Use .equals() for null-safe enum comparison
            if (!evTeam.equals(targetType)) continue;

            EventVolunteerStatus s = ev.getStatus();
            if (s == null) continue;
            // Accept ASSIGNED or COMPLETED (both mean "was there when event finished")
            if (s != EventVolunteerStatus.COMPLETED && s != EventVolunteerStatus.ASSIGNED) continue;

            Event e = ev.getEvent();
            OffsetDateTime completedAt = ev.getLeftAt() != null ? ev.getLeftAt() : e.getClosedAt();
            Neighborhood nbhd = e.getNeighborhood();
            District dist = nbhd != null ? nbhd.getDistrict() : null;
            result.add(new CompletedTask(
                    e.getId(), e.getTitle(),
                    dist != null ? dist.getName() : null,
                    nbhd != null ? nbhd.getName() : null,
                    nbhd, dist, ev.getStatus().name(), completedAt));
        }

        for (EventAssignment ea : eaHistory) {
            if (ea.getEvent() == null) continue;
            EventStatus eventStatus = ea.getEvent().getStatus();
            if (eventStatus == null || eventStatus != EventStatus.COMPLETED) continue;
            if (ea.getEvent().getTeam() == null) continue;
            TeamName eaTeam = ea.getEvent().getTeam().getName();
            if (eaTeam == null || !eaTeam.equals(targetType)) continue;

            Event e = ea.getEvent();
            Neighborhood nbhd = e.getNeighborhood();
            District dist = nbhd != null ? nbhd.getDistrict() : null;
            OffsetDateTime completedAt = ea.getAcceptedAt() != null ? ea.getAcceptedAt() : e.getClosedAt();
            result.add(new CompletedTask(
                    e.getId(), e.getTitle(),
                    dist != null ? dist.getName() : null,
                    nbhd != null ? nbhd.getName() : null,
                    nbhd, dist, "ACCEPTED_INVITATION", completedAt));
        }

        return result;
    }

    private boolean isCurrentlyActiveOnAnotherEvent(List<EventVolunteer> evHistory, UUID linkedEventId) {
        return evHistory.stream()
                .filter(ev -> ev.getStatus() == EventVolunteerStatus.ASSIGNED)
                .anyMatch(ev -> {
                    Event e = ev.getEvent();
                    if (e == null) return false;
                    EventStatus st = e.getStatus();
                    if (st == null) return false;
                    boolean activeStatus = st == EventStatus.IN_PROGRESS || st == EventStatus.OPEN;
                    boolean isThisEvent = linkedEventId != null && linkedEventId.equals(e.getId());
                    return activeStatus && !isThisEvent;
                });
    }

    private ProximityResult computeProximity(List<CompletedTask> completed, District targetDistrict,
                                              Neighborhood targetNeighborhood, Set<String> neighborNames) {
        // 1. Same neighborhood
        if (targetNeighborhood != null) {
            Optional<CompletedTask> match = completed.stream()
                    .filter(t -> t.neighborhood() != null
                            && t.neighborhood().getId().equals(targetNeighborhood.getId()))
                    .findFirst();
            if (match.isPresent()) {
                String label = formatTaskLabel(match.get());
                return new ProximityResult(SCORE_SAME_NEIGHBORHOOD, "SAME_NEIGHBORHOOD",
                        label + " (aynı mahalle)", label);
            }
        }

        // 2. Same district
        Optional<CompletedTask> sameDistMatch = completed.stream()
                .filter(t -> t.district() != null
                        && t.district().getId().equals(targetDistrict.getId()))
                .findFirst();
        if (sameDistMatch.isPresent()) {
            String label = formatTaskLabel(sameDistMatch.get());
            return new ProximityResult(SCORE_SAME_DISTRICT, "SAME_DISTRICT",
                    label + " (aynı ilçe)", label);
        }

        // 3. Neighbor district
        Optional<CompletedTask> neighborMatch = completed.stream()
                .filter(t -> t.district() != null
                        && neighborNames.contains(t.district().getName()))
                .findFirst();
        if (neighborMatch.isPresent()) {
            String label = formatTaskLabel(neighborMatch.get());
            return new ProximityResult(SCORE_NEIGHBOR_DISTRICT, "NEIGHBOR_DISTRICT",
                    label + " (komşu ilçe)", label);
        }

        // 4. Istanbul-wide
        String label = formatTaskLabel(completed.get(0));
        return new ProximityResult(SCORE_ISTANBUL_WIDE, "ISTANBUL_WIDE",
                label + " (İstanbul geneli)", label);
    }

    private String formatTaskLabel(CompletedTask t) {
        if (t.districtName() != null && t.neighborhoodName() != null)
            return t.districtName() + " / " + t.neighborhoodName();
        if (t.districtName() != null) return t.districtName();
        return t.title() != null ? t.title() : "Bilinmiyor";
    }

    // ── DEBUG CANDIDATES ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> debugCandidates(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        TeamName teamType  = event.getTeam() != null ? event.getTeam().getName() : null;
        District district  = event.getNeighborhood() != null ? event.getNeighborhood().getDistrict() : null;
        Neighborhood nbhd  = event.getNeighborhood();
        Set<String> nbNames = district != null ? neighborService.getNeighbors(district.getName()) : Set.of();

        // ALL active users (rol fark etmeksizin) — debug admin/coordinator exclusion'ı da gösterir
        List<User> allUsers = userRepository.findAllActiveAndVerified();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : allUsers) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("email",         u.getEmail());
            entry.put("name",          u.getFirstName() + " " + u.getLastName());
            entry.put("role",          u.getRole().name());
            entry.put("roleCheck",        u.getRole() == UserRole.VOLUNTEER);
            entry.put("activeCheck",      u.isActive());
            entry.put("hasEmail",         u.getEmail() != null && !u.getEmail().isBlank());

            if (u.getRole() != UserRole.VOLUNTEER) {
                entry.put("included",      false);
                entry.put("excludedReason","ROLE_NOT_VOLUNTEER");
                entry.put("score",          0);
                entry.put("matchedHistory", List.of());
                result.add(entry);
                continue;
            }

            if (teamType == null) {
                entry.put("included",      false);
                entry.put("excludedReason","EVENT_HAS_NO_TEAM_TYPE");
                result.add(entry);
                continue;
            }

            List<EventVolunteer> evHistory = eventVolunteerRepository.findByUserIdAll(u.getId());
            List<EventAssignment> eaHistory = eventAssignmentRepository
                    .findByUserIdAndStatusWithEvent(u.getId(), EventInvitationStatus.ACCEPTED);

            // Full task history for debug
            List<Map<String, Object>> allHistory = new ArrayList<>();
            for (EventVolunteer ev : evHistory) {
                if (ev.getEvent() == null) continue;
                Map<String, Object> h = new LinkedHashMap<>();
                h.put("source",              "EventVolunteer");
                h.put("eventId",             ev.getEvent().getId());
                h.put("eventTitle",          ev.getEvent().getTitle());
                h.put("eventStatus",         ev.getEvent().getStatus());
                h.put("teamName",            ev.getEvent().getTeam() != null ? ev.getEvent().getTeam().getName() : null);
                h.put("teamMatchesTarget",   ev.getEvent().getTeam() != null
                        && ev.getEvent().getTeam().getName() != null
                        && ev.getEvent().getTeam().getName().equals(teamType));
                h.put("volunteerStatus",     ev.getStatus());
                h.put("isEligible",
                        ev.getEvent().getStatus() == EventStatus.COMPLETED
                        && ev.getEvent().getTeam() != null
                        && ev.getEvent().getTeam().getName() != null
                        && ev.getEvent().getTeam().getName().equals(teamType)
                        && (ev.getStatus() == EventVolunteerStatus.COMPLETED
                            || ev.getStatus() == EventVolunteerStatus.ASSIGNED));
                h.put("leftAt",              ev.getLeftAt());
                allHistory.add(h);
            }
            for (EventAssignment ea : eaHistory) {
                if (ea.getEvent() == null) continue;
                Map<String, Object> h = new LinkedHashMap<>();
                h.put("source",              "EventAssignment");
                h.put("eventId",             ea.getEvent().getId());
                h.put("eventTitle",          ea.getEvent().getTitle());
                h.put("eventStatus",         ea.getEvent().getStatus());
                h.put("teamName",            ea.getEvent().getTeam() != null ? ea.getEvent().getTeam().getName() : null);
                h.put("teamMatchesTarget",   ea.getEvent().getTeam() != null
                        && ea.getEvent().getTeam().getName() != null
                        && ea.getEvent().getTeam().getName().equals(teamType));
                h.put("assignmentStatus",    ea.getStatus());
                h.put("isEligible",
                        ea.getEvent().getStatus() == EventStatus.COMPLETED
                        && ea.getEvent().getTeam() != null
                        && ea.getEvent().getTeam().getName() != null
                        && ea.getEvent().getTeam().getName().equals(teamType));
                allHistory.add(h);
            }
            entry.put("allTaskHistory", allHistory);

            // STEP 4
            List<CompletedTask> completedSameType = getCompletedSameTypeTasks(evHistory, eaHistory, teamType);
            entry.put("sameTaskCompletedCheck", !completedSameType.isEmpty());
            entry.put("completedSameTypeCount",  completedSameType.size());

            if (completedSameType.isEmpty()) {
                entry.put("included",      false);
                entry.put("excludedReason","NO_COMPLETED_SAME_TASK_HISTORY");
                entry.put("score",          0);
                entry.put("matchedHistory", buildMatchedHistory(completedSameType, teamType, district, nbhd, nbNames));
                result.add(entry);
                continue;
            }

            // STEP 5
            boolean currentlyActive = isCurrentlyActiveOnAnotherEvent(evHistory, eventId);
            entry.put("activeAssignmentCheck", !currentlyActive);
            if (currentlyActive) {
                entry.put("included",      false);
                entry.put("excludedReason","CURRENTLY_ACTIVE_ON_ANOTHER_EVENT");
                entry.put("score",          0);
                result.add(entry);
                continue;
            }

            // STEP 6
            boolean alreadyJoined = eventVolunteerRepository
                    .existsByEventIdAndUserIdAndStatus(eventId, u.getId(), EventVolunteerStatus.ASSIGNED);
            boolean alreadyInvited = eventAssignmentRepository.existsByEventIdAndUserId(eventId, u.getId());
            entry.put("sameEventCheck", !alreadyJoined && !alreadyInvited);
            if (alreadyJoined) {
                entry.put("included",      false);
                entry.put("excludedReason","ALREADY_JOINED_THIS_EVENT");
                result.add(entry);
                continue;
            }
            if (alreadyInvited) {
                entry.put("included",      false);
                entry.put("excludedReason","ALREADY_INVITED_THIS_EVENT");
                result.add(entry);
                continue;
            }

            // STEP 7-8
            ProximityResult prox = district != null
                    ? computeProximity(completedSameType, district, nbhd, nbNames)
                    : new ProximityResult(SCORE_ISTANBUL_WIDE, "ISTANBUL_WIDE", "İstanbul geneli", null);

            int score = prox.points();
            if (completedSameType.size() > 1) score += SCORE_MULTI_COMPLETIONS;
            OffsetDateTime t30 = OffsetDateTime.now().minusDays(30);
            boolean recent = completedSameType.stream()
                    .anyMatch(t -> t.completedAt() != null && t.completedAt().isAfter(t30));
            if (recent) score += SCORE_RECENT_30_DAY;

            entry.put("proximity",     prox.level());
            entry.put("score",         score);
            entry.put("included",      true);
            entry.put("excludedReason",null);
            entry.put("matchedHistory",buildMatchedHistory(completedSameType, teamType, district, nbhd, nbNames));
            entry.put("reasons",       List.of(prox.reason(),
                    completedSameType.size() > 1 ? completedSameType.size() + " tamamlanmış görev" : null,
                    recent ? "Son 30 günde tamamlandı" : null
            ).stream().filter(Objects::nonNull).toList());
            result.add(entry);
        }

        result.sort((a, b) -> {
            boolean ai = Boolean.TRUE.equals(a.get("included"));
            boolean bi = Boolean.TRUE.equals(b.get("included"));
            if (ai != bi) return bi ? 1 : -1;
            int as = a.get("score") instanceof Integer i ? i : 0;
            int bs = b.get("score") instanceof Integer i ? i : 0;
            return Integer.compare(bs, as);
        });
        return result;
    }

    private List<Map<String, Object>> buildMatchedHistory(List<CompletedTask> tasks, TeamName teamType,
                                                           District targetDistrict, Neighborhood targetNeighborhood,
                                                           Set<String> neighborNames) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CompletedTask t : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("eventTitle",          t.title());
            m.put("district",            t.districtName());
            m.put("neighborhood",        t.neighborhoodName());
            m.put("taskType",            teamType.name());
            m.put("participationStatus", t.participationStatus());
            m.put("proximityLevel",      computeTaskProximity(t, targetDistrict, targetNeighborhood, neighborNames));
            list.add(m);
        }
        return list;
    }

    private String computeTaskProximity(CompletedTask t, District targetDistrict,
                                         Neighborhood targetNeighborhood, Set<String> neighborNames) {
        if (targetNeighborhood != null && t.neighborhood() != null
                && t.neighborhood().getId().equals(targetNeighborhood.getId()))
            return "SAME_NEIGHBORHOOD";
        if (t.district() != null && targetDistrict != null
                && t.district().getId().equals(targetDistrict.getId()))
            return "SAME_DISTRICT";
        if (t.district() != null && neighborNames.contains(t.district().getName()))
            return "NEIGHBOR_DISTRICT";
        return "ISTANBUL_GENERAL";
    }

    // ── Approve / Reject ──────────────────────────────────────────────────────

    @Transactional
    public ApproveRecommendationResponse approveWithSelectedMembers(UUID id,
                                                                     ApproveRecommendationRequest req,
                                                                     UserPrincipal principal) {
        TeamRecommendation rec = getOrThrow(id);
        if (rec.getStatus() != RecommendationStatus.DRAFT)
            throw new IllegalStateException("Yalnızca DRAFT durumdaki öneriler onaylanabilir.");
        if (principal.getRole() != UserRole.ADMIN && principal.getRole() != UserRole.DISTRICT_COORDINATOR)
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");

        User approvedBy = userRepository.findById(principal.getId()).orElseThrow();
        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setApprovedBy(approvedBy);
        rec.setApprovedAt(OffsetDateTime.now());
        recommendationRepository.save(rec);

        Event event = rec.getEvent();
        String managerName = approvedBy.getFirstName() + " " + approvedBy.getLastName();

        List<EventAssignmentResponse> assignmentResponses = new ArrayList<>();
        int mailSent = 0, mailFailed = 0;

        for (UUID userId : req.selectedUserIds()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            String acceptToken = generateSecureToken();
            String declineToken = generateSecureToken();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

            EventAssignment existing = event != null
                    ? eventAssignmentRepository.findByEventIdAndUserId(event.getId(), userId).orElse(null)
                    : null;
            EventAssignment assignment;
            if (existing != null) {
                existing.setStatus(EventInvitationStatus.INVITED);
                existing.setInvitationToken(acceptToken);
                existing.setTokenExpiresAt(expiresAt);
                existing.setInvitedAt(OffsetDateTime.now());
                existing.setAcceptedAt(null);
                existing.setDeclinedAt(null);
                assignment = eventAssignmentRepository.save(existing);
            } else {
                assignment = eventAssignmentRepository.save(EventAssignment.builder()
                        .event(event).user(user).invitedBy(approvedBy)
                        .status(EventInvitationStatus.INVITED)
                        .invitationToken(acceptToken).tokenExpiresAt(expiresAt)
                        .recommendation(rec).invitedAt(OffsetDateTime.now()).build());
            }

            boolean sent = false;
            if (event != null) {
                try {
                    emailNotificationService.sendEventAssignmentInvitation(
                            user, event, acceptToken, declineToken, managerName);
                    sent = true; mailSent++;
                } catch (Exception e) {
                    mailFailed++;
                    log.warn("Davet maili gönderilemedi userId={}: {}", userId, e.getMessage());
                }
            }

            assignmentResponses.add(EventAssignmentResponse.builder()
                    .id(assignment.getId()).userId(user.getId())
                    .firstName(user.getFirstName()).lastName(user.getLastName())
                    .email(user.getEmail()).status(assignment.getStatus().name())
                    .invitedAt(assignment.getInvitedAt()).mailSent(sent).build());
        }

        List<TeamRecommendationMember> members = memberRepository.findByRecommendationIdOrderByOrderIdAsc(id);
        return ApproveRecommendationResponse.builder()
                .recommendation(toResponse(rec, members)).assignments(assignmentResponses)
                .mailSentCount(mailSent).mailFailedCount(mailFailed)
                .mailNote(mailFailed > 0 ? mailFailed + " kişiye mail gönderilemedi." : null)
                .build();
    }

    @Transactional
    public TeamRecommendationResponse approve(UUID id, UserPrincipal principal) {
        TeamRecommendation rec = getOrThrow(id);
        if (rec.getStatus() != RecommendationStatus.DRAFT)
            throw new IllegalStateException("Yalnızca DRAFT durumdaki öneriler onaylanabilir.");
        if (principal.getRole() != UserRole.ADMIN && principal.getRole() != UserRole.DISTRICT_COORDINATOR)
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");

        User approvedBy = userRepository.findById(principal.getId()).orElseThrow();
        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setApprovedBy(approvedBy);
        rec.setApprovedAt(OffsetDateTime.now());
        recommendationRepository.save(rec);

        List<TeamRecommendationMember> members = memberRepository.findByRecommendationIdOrderByOrderIdAsc(id);
        int mailErrors = 0;
        for (TeamRecommendationMember m : members) {
            try {
                emailNotificationService.sendTeamRecommendationApproved(m.getUser(),
                        rec.getTeamType().getLabel(), rec.getDistrict().getName(),
                        rec.getNeighborhood() != null ? rec.getNeighborhood().getName() : null,
                        rec.getPriority());
            } catch (Exception e) {
                mailErrors++;
                log.warn("Mail gönderilemedi userId={}: {}", m.getUser().getId(), e.getMessage());
            }
        }
        TeamRecommendationResponse resp = toResponse(rec, members);
        if (mailErrors > 0)
            resp.setAiExplanation((resp.getAiExplanation() != null ? resp.getAiExplanation() : "")
                    + "\n\n[Bilgi: " + mailErrors + " kişiye mail gönderilemedi.]");
        return resp;
    }

    @Transactional
    public TeamRecommendationResponse reject(UUID id, UserPrincipal principal) {
        TeamRecommendation rec = getOrThrow(id);
        if (rec.getStatus() != RecommendationStatus.DRAFT)
            throw new IllegalStateException("Yalnızca DRAFT durumdaki öneriler reddedilebilir.");
        rec.setStatus(RecommendationStatus.REJECTED);
        recommendationRepository.save(rec);
        return toResponse(rec, memberRepository.findByRecommendationIdOrderByOrderIdAsc(id));
    }

    // ── List / Get ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TeamRecommendationResponse> list(RecommendationStatus status, int page, int size,
                                                  UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<TeamRecommendation> recs;
        if (principal.getRole() == UserRole.ADMIN) {
            recs = status != null
                    ? recommendationRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    : recommendationRepository.findAll(pageable);
        } else {
            UUID districtId = principal.getDistrictId();
            recs = status != null && districtId != null
                    ? recommendationRepository.findByDistrictIdAndStatusOrderByCreatedAtDesc(districtId, status, pageable)
                    : recommendationRepository.findByRequestedByIdOrderByCreatedAtDesc(principal.getId(), pageable);
        }
        return recs.map(rec -> toResponse(rec, memberRepository.findByRecommendationIdOrderByOrderIdAsc(rec.getId())));
    }

    @Transactional(readOnly = true)
    public TeamRecommendationResponse getById(UUID id) {
        TeamRecommendation rec = getOrThrow(id);
        return toResponse(rec, memberRepository.findByRecommendationIdOrderByOrderIdAsc(id));
    }

    @Transactional(readOnly = true)
    public Optional<TeamRecommendationResponse> getLatestForEvent(UUID eventId) {
        return recommendationRepository.findTopByEventIdOrderByCreatedAtDesc(eventId)
                .map(rec -> toResponse(rec, memberRepository.findByRecommendationIdOrderByOrderIdAsc(rec.getId())));
    }

    // ── AI explanation ────────────────────────────────────────────────────────

    private String buildAiExplanation(TeamName teamType, District district, Neighborhood neighborhood,
                                       int requiredSize, List<ScoredCandidate> top) {
        if (top.isEmpty()) {
            return "Bu görev tipi için daha önce başarıyla tamamlanmış görevi olan uygun gönüllü bulunamadı.";
        }
        if (apiKey == null || apiKey.isBlank()) {
            return "AI servisi yapılandırılmamış. " + top.size() + " uygun gönüllü algoritma ile seçildi.";
        }
        try {
            return callClaudeApi(buildPrompt(teamType, district, neighborhood, requiredSize, top));
        } catch (Exception e) {
            log.warn("AI explanation failed: {}", e.getMessage());
            return "AI açıklaması oluşturulamadı. " + top.size() + " uygun gönüllü algoritma ile seçildi.";
        }
    }

    private String buildPrompt(TeamName teamType, District district, Neighborhood neighborhood,
                                int requiredSize, List<ScoredCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Yalnızca backend tarafından seçilmiş eligible VOLUNTEER adayları açıkla.\n")
          .append("Yeni aday ekleme. Yönetici veya koordinatör önerme.\n\n")
          .append("GÖREV: ").append(teamType.getLabel())
          .append(" / ").append(district.getName());
        if (neighborhood != null) sb.append(" / ").append(neighborhood.getName());
        sb.append(" / ").append(requiredSize).append(" kişi\n\nADAYLAR:\n");
        for (ScoredCandidate c : candidates) {
            sb.append("  - ").append(c.user().getFirstName()).append(" ").append(c.user().getLastName())
              .append(" skor=").append(c.score())
              .append(" yakınlık=").append(c.proximityLevel())
              .append(" öncekiGörev=").append(c.previousSimilarTask() != null ? c.previousSimilarTask() : "-")
              .append("\n");
        }
        sb.append("\nKısa operasyonel gerekçe yaz (Türkçe, 3-5 cümle). Kesin karar verme.");
        return sb.toString();
    }

    private String callClaudeApi(String message) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model).put("max_tokens", 500)
            .put("system", "Sen afet koordinasyon sistemi için ekip öneri gerekçesi yazan bir analistsin. Kısa, operasyonel ve Türkçe yaz.");
        ObjectNode msg = body.putArray("messages").addObject();
        msg.put("role", "user").put("content", message);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Claude API HTTP " + response.statusCode());
        return objectMapper.readTree(response.body()).at("/content/0/text").asText();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private TeamRecommendationResponse toResponse(TeamRecommendation rec,
                                                   List<TeamRecommendationMember> members) {
        return TeamRecommendationResponse.builder()
                .id(rec.getId())
                .eventId(rec.getEvent() != null ? rec.getEvent().getId() : null)
                .teamType(rec.getTeamType().name())
                .teamTypeLabel(rec.getTeamType().getLabel())
                .districtName(rec.getDistrict().getName())
                .neighborhoodName(rec.getNeighborhood() != null ? rec.getNeighborhood().getName() : null)
                .requiredTeamSize(rec.getRequiredTeamSize())
                .priority(rec.getPriority())
                .status(rec.getStatus().name())
                .aiExplanation(rec.getAiExplanation())
                .recommendedPersonnel(members.stream()
                        .map(m -> RecommendedMemberResponse.builder()
                                .userId(m.getUser().getId())
                                .firstName(m.getUser().getFirstName())
                                .lastName(m.getUser().getLastName())
                                .email(m.getUser().getEmail())
                                .score(m.getScore()).orderId(m.getOrderId())
                                .completedSimilarTasks(m.getCompletedSimilarTasks())
                                .reasons(m.getReasons() != null
                                        ? Arrays.asList(m.getReasons().split("\\|")) : List.of())
                                .proximityLevel(m.getProximityLevel())
                                .availability(m.getAvailability())
                                .previousSimilarTask(m.getPreviousSimilarTask())
                                .build())
                        .toList())
                .requestedByName(rec.getRequestedBy().getFirstName() + " " + rec.getRequestedBy().getLastName())
                .approvedByName(rec.getApprovedBy() != null
                        ? rec.getApprovedBy().getFirstName() + " " + rec.getApprovedBy().getLastName() : null)
                .approvedAt(rec.getApprovedAt())
                .createdAt(rec.getCreatedAt())
                .build();
    }

    private TeamRecommendation getOrThrow(UUID id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeamRecommendation", "id", id));
    }

    private TeamName parseTeamType(String value) {
        try {
            return TeamName.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Geçersiz ekip tipi: " + value);
        }
    }

    // ── Inner records ─────────────────────────────────────────────────────────

    private record ScoredCandidate(User user, int score, int completedSimilarTasks,
                                   List<String> reasons, String proximityLevel,
                                   String availability, String previousSimilarTask) {}

    private record ProximityResult(int points, String level, String reason, String sampleTask) {}

    private record CompletedTask(UUID eventId, String title, String districtName, String neighborhoodName,
                                  Neighborhood neighborhood, District district,
                                  String participationStatus, OffsetDateTime completedAt) {}
}
