package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.entity.ResourceRequest;
import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.domain.enums.ResourceRequestStatus;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EventRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Risk score formula:
 *   riskScore = (teamRawScore × 0.45) + (damageRawScore × 0.30) + (resourceRawScore × 0.25)
 *
 * Team raw score:  sum of (teamTypePuan × event.requiredPeople) for active events
 * Damage raw score: sum of damageLevelPuan for KOORDINATOR_ONAYLADI assessments only
 * Resource raw score: activeRequestCount × 3  (OPEN + IN_PROGRESS)
 *
 * Colors: ≤100 GREEN | 101-200 YELLOW | 201-300 RED | >300 PURPLE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationService {

    // ── Team type points per required person ──────────────────────────────────
    private static final Map<TeamName, Integer> TEAM_POINTS = Map.of(
            TeamName.SEARCH_RESCUE,       5,
            TeamName.FOOD_WATER,          3,
            TeamName.EVACUATION,          2,
            TeamName.COMMUNICATION,       2,
            TeamName.PSYCHOSOCIAL,        1,
            TeamName.HASAR_TESPIT_EKIBI,  2,
            TeamName.LOGISTICS,           1,
            TeamName.OTHER,               1
    );

    // ── Damage level points (only KOORDINATOR_ONAYLADI records count) ─────────
    private static final Map<DamageLevel, Integer> DAMAGE_POINTS = Map.of(
            DamageLevel.COLLAPSED,  5,
            DamageLevel.HEAVY,      4,
            DamageLevel.MODERATE,   3,
            DamageLevel.LIGHT,      2,
            DamageLevel.UNASSESSED, 0
    );

    // ── Formula weights ───────────────────────────────────────────────────────
    private static final double WEIGHT_TEAM     = 0.45;
    private static final double WEIGHT_DAMAGE   = 0.30;
    private static final double WEIGHT_RESOURCE = 0.25;
    private static final int    RESOURCE_POINTS = 3;

    // ── Risk color thresholds ─────────────────────────────────────────────────
    private static final double THRESHOLD_GREEN  = 100.0;
    private static final double THRESHOLD_YELLOW = 200.0;
    private static final double THRESHOLD_RED    = 300.0;

    // ── Active status lists ───────────────────────────────────────────────────
    private static final List<EventStatus> ACTIVE_EVENT_STATUSES =
            List.of(EventStatus.OPEN, EventStatus.IN_PROGRESS);

    private static final List<ResourceRequestStatus> ACTIVE_RESOURCE_STATUSES =
            List.of(ResourceRequestStatus.OPEN, ResourceRequestStatus.IN_PROGRESS);

    private final EventRepository eventRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final DistrictRepository districtRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;

    // ── Public recalculation entry points ─────────────────────────────────────

    @Transactional
    public void recalculateNeighborhoodRisk(UUID neighborhoodId) {
        List<Event> events = eventRepository
                .findActiveByNeighborhoodId(neighborhoodId, ACTIVE_EVENT_STATUSES);
        List<DamageAssessment> damages = damageAssessmentRepository
                .findByNeighborhoodIdAndVerificationStatus(neighborhoodId, VerificationStatus.KOORDINATOR_ONAYLADI);
        List<ResourceRequest> resources = resourceRequestRepository
                .findByNeighborhoodIdAndStatusIn(neighborhoodId, ACTIVE_RESOURCE_STATUSES);

        double teamRaw     = calculateTeamRaw(events);
        double damageRaw   = calculateDamageRaw(damages);
        double resourceRaw = resources.size() * (double) RESOURCE_POINTS;
        double score       = (teamRaw * WEIGHT_TEAM) + (damageRaw * WEIGHT_DAMAGE) + (resourceRaw * WEIGHT_RESOURCE);

        log.info("Risk recalculation [neighborhood={}] events={} teamRaw={} damageRaw={} resourceRaw={} score={}",
                neighborhoodId, events.size(), teamRaw, damageRaw, resourceRaw, score);

        BigDecimal bigScore = BigDecimal.valueOf(score);
        neighborhoodRepository.findById(neighborhoodId).ifPresent(n -> {
            n.setRiskScore(bigScore);
            n.setRiskScoreUpdatedAt(OffsetDateTime.now());
            neighborhoodRepository.save(n);
            log.info("Risk saved [neighborhood={} name={}] score={} color={}",
                    neighborhoodId, n.getName(), score, getRiskColor(bigScore));
        });
    }

    @Transactional
    public void recalculateDistrictRisk(UUID districtId) {
        List<Event> events = eventRepository.findAllByDistrictId(districtId)
                .stream()
                .filter(e -> ACTIVE_EVENT_STATUSES.contains(e.getStatus()))
                .toList();
        List<DamageAssessment> damages = damageAssessmentRepository
                .findByDistrictIdAndVerificationStatus(districtId, VerificationStatus.KOORDINATOR_ONAYLADI);
        List<ResourceRequest> resources = resourceRequestRepository
                .findByDistrictIdAndStatusIn(districtId, ACTIVE_RESOURCE_STATUSES);

        double teamRaw     = calculateTeamRaw(events);
        double damageRaw   = calculateDamageRaw(damages);
        double resourceRaw = resources.size() * (double) RESOURCE_POINTS;
        double score       = (teamRaw * WEIGHT_TEAM) + (damageRaw * WEIGHT_DAMAGE) + (resourceRaw * WEIGHT_RESOURCE);

        log.info("Risk recalculation [district={}] events={} teamRaw={} damageRaw={} resourceRaw={} score={}",
                districtId, events.size(), teamRaw, damageRaw, resourceRaw, score);

        BigDecimal bigScore = BigDecimal.valueOf(score);
        districtRepository.findById(districtId).ifPresent(d -> {
            d.setRiskScore(bigScore);
            d.setRiskScoreUpdatedAt(OffsetDateTime.now());
            districtRepository.save(d);
            log.info("Risk saved [district={} name={}] score={} color={}",
                    districtId, d.getName(), score, getRiskColor(bigScore));
        });
    }

    // ── Color / level helpers ─────────────────────────────────────────────────

    public String getRiskColor(BigDecimal riskScore) {
        if (riskScore == null) return "GREEN";
        double score = riskScore.doubleValue();
        if (score <= THRESHOLD_GREEN)  return "GREEN";
        if (score <= THRESHOLD_YELLOW) return "YELLOW";
        if (score <= THRESHOLD_RED)    return "RED";
        return "PURPLE";
    }

    public String getRiskLevel(BigDecimal riskScore) {
        if (riskScore == null) return "LOW";
        double score = riskScore.doubleValue();
        if (score <= THRESHOLD_GREEN)  return "LOW";
        if (score <= THRESHOLD_YELLOW) return "MEDIUM";
        if (score <= THRESHOLD_RED)    return "HIGH";
        return "CRITICAL";
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private double calculateTeamRaw(List<Event> events) {
        double total = 0.0;
        for (Event event : events) {
            int pts = teamPoints(event);
            double contribution = (double) pts * event.getRequiredPeople();
            log.debug("  Event [{}] team={} pts={} people={} contribution={}",
                    event.getId(),
                    event.getTeam() != null && event.getTeam().getName() != null
                            ? event.getTeam().getName() : "UNKNOWN",
                    pts, event.getRequiredPeople(), contribution);
            total += contribution;
        }
        return total;
    }

    private double calculateDamageRaw(List<DamageAssessment> damages) {
        double total = 0.0;
        for (DamageAssessment da : damages) {
            total += DAMAGE_POINTS.getOrDefault(da.getDamageLevel(), 0);
        }
        return total;
    }

    private int teamPoints(Event event) {
        if (event.getTeam() == null) return 1;
        TeamName name = event.getTeam().getName();
        return name == null ? 1 : TEAM_POINTS.getOrDefault(name, 1);
    }
}
