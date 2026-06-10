package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.ResourceStock;
import com.afet.koordinasyon.domain.enums.*;
import com.afet.koordinasyon.dto.response.report.*;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Rapor Merkezi'nin 8 rapor türünü üretir (DAILY/WEEKLY/MONTHLY kapsamlı + OPERATION/RESOURCE/
 * STOCK/DAMAGE/VOLUNTEER özel). Rol bazlı ilçe/mahalle kapsamı BACKEND'de zorlanır; dönem
 * createdAt pencereli, stok seviyeleri anlık snapshot'tır. Tüm sayımlar aggregate sorgularla
 * hesaplanır; veri kaynağı olmayan alanlar null bırakılır ("Veri bulunamadı").
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedReportService {

    private final ResourceRequestRepository resourceRequestRepository;
    private final ResourceStockRepository resourceStockRepository;
    private final ResourceStockMovementRepository movementRepository;
    private final ResourceStockService resourceStockService;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final EventRepository eventRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final TeamRepository teamRepository;
    private final AuditLogService auditLogService;

    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(TR);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(TR);
    private static final List<EventStatus> ACTIVE_EVENTS = List.of(EventStatus.OPEN, EventStatus.IN_PROGRESS);
    private static final List<ResourceRequestStatus> ALL_REQ_STATUSES = List.of(
            ResourceRequestStatus.OPEN, ResourceRequestStatus.IN_PROGRESS,
            ResourceRequestStatus.FULFILLED, ResourceRequestStatus.CANCELLED);

    // ── Kapsamlı rapor (DAILY/WEEKLY/MONTHLY) ─────────────────────────────────

    @Transactional(readOnly = true)
    public ComprehensiveReportResponse generateComprehensive(ReportType type, LocalDate date,
                                                             UUID districtId, UUID neighborhoodId,
                                                             UserPrincipal principal) {
        Scope scope = resolveScope(districtId, neighborhoodId, principal);
        Period period = resolvePeriod(type, date);
        ReportMeta meta = buildMeta(type, period, scope, principal);

        ReportModules.TeamModule team = buildTeamModule(scope, period);
        ReportModules.DamageModule damage = buildDamageModule(scope, period);
        ReportModules.ResourceModule resource = buildResourceModule(scope, period);
        ReportModules.StockModule stock = buildStockModule(scope, period);
        ReportModules.DocumentModule document = buildDocumentModule(scope, period);
        ReportModules.CoordinatorModule coordinator = buildCoordinatorModule(scope, period);
        ReportModules.VolunteerModule volunteer = buildVolunteerModule(scope, period);

        List<String> kpis = buildKpis(team, damage, resource, stock, volunteer);
        String summary = buildComprehensiveSummary(type, scope, team, damage, resource, stock, volunteer);

        audit(principal, type, scope);
        return ComprehensiveReportResponse.builder()
                .meta(meta).team(team).damage(damage).resource(resource).stock(stock)
                .document(document).coordinator(coordinator).volunteer(volunteer)
                .kpis(kpis).managerSummary(summary).build();
    }

    // ── Özel raporlar ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OperationReportResponse generateOperation(LocalDate date, UUID d, UUID n, UserPrincipal p) {
        Scope scope = resolveScope(d, n, p);
        Period period = resolvePeriod(ReportType.OPERATION, date);
        ReportModules.TeamModule team = buildTeamModule(scope, period);
        audit(p, ReportType.OPERATION, scope);
        return OperationReportResponse.builder()
                .meta(buildMeta(ReportType.OPERATION, period, scope, p))
                .team(team)
                .managerSummary(buildOperationSummary(scope, team))
                .build();
    }

    @Transactional(readOnly = true)
    public ResourceReportResponse generateResource(LocalDate date, UUID d, UUID n, UserPrincipal p) {
        Scope scope = resolveScope(d, n, p);
        Period period = resolvePeriod(ReportType.RESOURCE, date);
        ReportModules.ResourceModule resource = buildResourceModule(scope, period);
        audit(p, ReportType.RESOURCE, scope);
        return ResourceReportResponse.builder()
                .meta(buildMeta(ReportType.RESOURCE, period, scope, p))
                .resource(resource)
                .managerSummary(buildResourceSummary(scope, resource))
                .build();
    }

    @Transactional(readOnly = true)
    public StockReportResponse generateStock(LocalDate date, UUID d, UUID n, UserPrincipal p) {
        Scope scope = resolveScope(d, n, p);
        Period period = resolvePeriod(ReportType.STOCK, date);
        ReportModules.StockModule stock = buildStockModule(scope, period);
        audit(p, ReportType.STOCK, scope);
        return StockReportResponse.builder()
                .meta(buildMeta(ReportType.STOCK, period, scope, p))
                .stock(stock)
                .supplyRecommendations(buildSupplyRecommendations(stock))
                .managerSummary(buildStockSummary(scope, stock))
                .build();
    }

    @Transactional(readOnly = true)
    public DamageReportResponse generateDamage(LocalDate date, UUID d, UUID n, UserPrincipal p) {
        Scope scope = resolveScope(d, n, p);
        Period period = resolvePeriod(ReportType.DAMAGE, date);
        ReportModules.DamageModule damage = buildDamageModule(scope, period);
        audit(p, ReportType.DAMAGE, scope);
        return DamageReportResponse.builder()
                .meta(buildMeta(ReportType.DAMAGE, period, scope, p))
                .damage(damage)
                .managerSummary(buildDamageSummary(scope, damage))
                .build();
    }

    @Transactional(readOnly = true)
    public VolunteerReportResponse generateVolunteer(LocalDate date, UUID d, UUID n, UserPrincipal p) {
        Scope scope = resolveScope(d, n, p);
        Period period = resolvePeriod(ReportType.VOLUNTEER, date);
        ReportModules.VolunteerModule volunteer = buildVolunteerModule(scope, period);
        audit(p, ReportType.VOLUNTEER, scope);
        return VolunteerReportResponse.builder()
                .meta(buildMeta(ReportType.VOLUNTEER, period, scope, p))
                .volunteer(volunteer)
                .managerSummary(buildVolunteerSummary(scope, volunteer))
                .build();
    }

    // ── Modül üreticileri ─────────────────────────────────────────────────────

    private ReportModules.TeamModule buildTeamModule(Scope s, Period period) {
        UUID d = s.districtId(), n = s.neighborhoodId();
        long totalTeams = teamRepository.count();
        long activeTeams = eventRepository.reportActiveTeams(EventStatus.COMPLETED, d, n);
        long required = eventRepository.reportRequiredPeopleSum(ACTIVE_EVENTS, d, n);
        long assigned = eventVolunteerRepository.reportAssignedVolunteers(d, n);
        List<ReportModules.CategoryCount> breakdown = new ArrayList<>();
        for (Object[] row : eventRepository.reportEventCountByTeam(d, n)) {
            if (row[0] == null) continue;
            TeamName tn = (TeamName) row[0];
            breakdown.add(new ReportModules.CategoryCount(tn.name(), tn.getLabel(), ((Number) row[1]).longValue()));
        }
        return ReportModules.TeamModule.builder()
                .totalTeams(totalTeams)
                .activeTeams(activeTeams)
                .availableTeams(Math.max(0, totalTeams - activeTeams))
                .totalTasks(eventRepository.reportCountAllEvents(d, n))
                .completedTasks(eventRepository.reportCountEventsByStatus(EventStatus.COMPLETED, d, n))
                .inProgressTasks(eventRepository.reportCountEventsByStatus(EventStatus.IN_PROGRESS, d, n))
                .openTasks(eventRepository.reportCountEventsByStatus(EventStatus.OPEN, d, n))
                .requiredPeople(required)
                .shortage(Math.max(0, required - assigned))
                .avgResponseMinutes(null)
                .avgCompletionMinutes(minutesOrNull(eventRepository.reportAvgResolutionSeconds(d, n)))
                .teamBreakdown(breakdown)
                .build();
    }

    private ReportModules.DamageModule buildDamageModule(Scope s, Period period) {
        UUID d = s.districtId(), n = s.neighborhoodId();
        long pending = countDamage(d, n, null, VerificationStatus.INCELEME_GEREKIYOR);
        return ReportModules.DamageModule.builder()
                .total(countDamage(d, n, null, null))
                .newInPeriod(damageAssessmentRepository.reportCountCreatedBetween(d, n, period.from(), period.to()))
                .pendingReview(pending)
                .fieldVerified(countDamage(d, n, null, VerificationStatus.SAHADA_DOGRULANDI))
                .coordinatorApproved(countDamage(d, n, null, VerificationStatus.KOORDINATOR_ONAYLADI))
                .assignedTeam(countDamage(d, n, null, VerificationStatus.ASSIGNED))
                .teamNeeded(pending)
                .unassigned(pending)
                .lightRisk(countDamage(d, n, DamageLevel.LIGHT, null))
                .moderateRisk(countDamage(d, n, DamageLevel.MODERATE, null))
                .heavyRisk(countDamage(d, n, DamageLevel.HEAVY, null))
                .criticalRisk(countDamage(d, n, DamageLevel.COLLAPSED, null))
                .avgReviewMinutes(null)
                .topNeighborhoods(neighborhoodMetrics(damageAssessmentRepository.reportDamageCountByNeighborhood(d), 5))
                .build();
    }

    private ReportModules.ResourceModule buildResourceModule(Scope s, Period period) {
        UUID d = s.districtId(), n = s.neighborhoodId();
        long total = countRequests(d, n, null, null);
        long fulfilled = countRequests(d, n, ResourceRequestStatus.FULFILLED, null);
        List<ReportModules.CategoryCount> categories = new ArrayList<>();
        for (Object[] row : resourceTypeSummary(d, n)) {
            if (row[0] == null) continue;
            ResourceType rt = (ResourceType) row[0];
            categories.add(new ReportModules.CategoryCount(rt.name(), rt.getLabel(), ((Number) row[1]).longValue()));
        }
        return ReportModules.ResourceModule.builder()
                .total(total)
                .fulfilled(fulfilled)
                .pending(countRequests(d, n, ResourceRequestStatus.OPEN, null))
                .partial(countRequests(d, n, ResourceRequestStatus.IN_PROGRESS, null))
                .rejected(countRequests(d, n, ResourceRequestStatus.CANCELLED, null))
                .fulfillmentRate(percent(fulfilled, total))
                .avgResolutionMinutes(minutesOrNull(resourceRequestRepository.reportAvgResolutionSeconds(d, n)))
                .criticalPending(countRequests(d, n, ResourceRequestStatus.OPEN, RequestPriority.CRITICAL))
                .topCategories(categories)
                .topNeighborhoods(neighborhoodMetrics(resourceRequestRepository.reportRequestCountByNeighborhood(d), 5))
                .build();
    }

    private ReportModules.StockModule buildStockModule(Scope s, Period period) {
        UUID d = s.districtId(), n = s.neighborhoodId();
        List<ResourceStock> stocks = resourceStockRepository.search(d, n, null, false, "%");
        long totalQty = 0, criticalItems = 0, outOfStock = 0, dailyUsage = 0;
        Map<ResourceType, Long> byCategory = new LinkedHashMap<>();
        Map<UUID, long[]> byNeighborhood = new LinkedHashMap<>(); // id -> [qty]
        Map<UUID, String> nbNames = new LinkedHashMap<>();
        Map<UUID, String> nbDistrictNames = new LinkedHashMap<>();
        for (ResourceStock st : stocks) {
            int qty = st.getQuantity();
            totalQty += qty;
            if (st.getDailyUsageEstimate() != null) dailyUsage += st.getDailyUsageEstimate();
            ResourceStockStatus status = resourceStockService.computeStatus(
                    st.getQuantity(), st.getCriticalThreshold(), st.getDailyUsageEstimate());
            if (status == ResourceStockStatus.OUT_OF_STOCK) outOfStock++;
            else if (status == ResourceStockStatus.CRITICAL) criticalItems++;
            byCategory.merge(st.getCategory(), (long) qty, Long::sum);
            if (st.getNeighborhood() != null) {
                UUID nid = st.getNeighborhood().getId();
                byNeighborhood.computeIfAbsent(nid, k -> new long[1])[0] += qty;
                nbNames.putIfAbsent(nid, st.getNeighborhood().getName());
                if (st.getNeighborhood().getDistrict() != null) {
                    nbDistrictNames.putIfAbsent(nid, st.getNeighborhood().getDistrict().getName());
                }
            }
        }
        List<ReportModules.CategoryCount> categoryDist = byCategory.entrySet().stream()
                .map(e -> new ReportModules.CategoryCount(e.getKey().name(), e.getKey().getLabel(), e.getValue()))
                .toList();
        List<ReportModules.NeighborhoodMetric> nbDist = byNeighborhood.entrySet().stream()
                .map(e -> new ReportModules.NeighborhoodMetric(
                        e.getKey().toString(),
                        nbNames.getOrDefault(e.getKey(), "Bilinmeyen"),
                        nbDistrictNames.get(e.getKey()),
                        e.getValue()[0]))
                .sorted((a, b) -> Long.compare(b.value(), a.value()))
                .limit(10).toList();

        long inflow = movementRepository.sumNetChangeBetween(d, n, period.from(), period.to());
        long outflow = movementRepository.sumConsumedBetween(d, n, period.from(), period.to());
        long inflowOnly = Math.max(0, inflow + outflow); // net = giriş - çıkış → giriş = net + çıkış
        Integer estDays = dailyUsage > 0 ? (int) Math.floor((double) totalQty / dailyUsage) : null;

        return ReportModules.StockModule.builder()
                .totalItems(stocks.size())
                .totalQuantity(totalQty)
                .availableQuantity(totalQty)
                .criticalItems(criticalItems)
                .outOfStockItems(outOfStock)
                .periodInflow(inflowOnly)
                .periodOutflow(outflow)
                .estimatedDaysRemaining(estDays)
                .categoryDistribution(categoryDist)
                .neighborhoodDistribution(nbDist)
                .build();
    }

    private ReportModules.DocumentModule buildDocumentModule(Scope s, Period period) {
        UUID d = s.districtId(), n = s.neighborhoodId();
        List<ReportModules.CategoryCount> types = new ArrayList<>();
        for (Object[] row : documentRepository.reportCountByTypeCreatedBetween(d, n, period.from(), period.to())) {
            if (row[0] == null) continue;
            DocumentType dt = (DocumentType) row[0];
            types.add(new ReportModules.CategoryCount(dt.name(), dt.name(), ((Number) row[1]).longValue()));
        }
        return ReportModules.DocumentModule.builder()
                .uploaded(documentRepository.reportCountCreatedBetween(d, n, period.from(), period.to()))
                .approved(documentRepository.reportCountReviewedBetween(DocumentStatus.APPROVED, d, n, period.from(), period.to()))
                .rejected(documentRepository.reportCountReviewedBetween(DocumentStatus.REJECTED, d, n, period.from(), period.to()))
                .pending(documentRepository.reportCountByStatus(DocumentStatus.PENDING, d, n))
                .typeDistribution(types)
                .build();
    }

    private ReportModules.CoordinatorModule buildCoordinatorModule(Scope s, Period period) {
        long assignments = auditLogRepository.countByActionAndCreatedAtBetween(
                AuditActionType.COORDINATOR_ASSIGNED.name(), period.from(), period.to());
        long neighborhoodCoords = s.districtId() != null
                ? neighborhoodRepository.countByCoordinatorIsNotNullAndDistrictId(s.districtId())
                : neighborhoodRepository.countByCoordinatorIsNotNull();
        return ReportModules.CoordinatorModule.builder()
                .assignmentsInPeriod(assignments)
                .districtCoordinators(districtRepository.countByCoordinatorIsNotNull())
                .neighborhoodCoordinators(neighborhoodCoords)
                .pendingProcesses(null)
                .build();
    }

    private ReportModules.VolunteerModule buildVolunteerModule(Scope s, Period period) {
        UUID d = s.districtId(), n = s.neighborhoodId();
        long total = userRepository.countVolunteersByRole(UserRole.VOLUNTEER, d, n);
        long active = userRepository.countActiveVolunteersByRole(UserRole.VOLUNTEER, d, n);
        long approved = userRepository.countApprovedVolunteers(UserRole.VOLUNTEER, d, n);
        return ReportModules.VolunteerModule.builder()
                .total(total)
                .newInPeriod(userRepository.countNewVolunteersBetween(UserRole.VOLUNTEER, d, n, period.from(), period.to()))
                .approved(approved)
                .pending(Math.max(0, total - approved))
                .active(active)
                .passive(Math.max(0, total - active))
                .assigned(eventVolunteerRepository.reportAssignedVolunteers(d, n))
                .rejected(null).trained(null).capacityGap(null)
                .neighborhoodDistribution(neighborhoodMetrics(
                        userRepository.reportVolunteerCountByNeighborhood(UserRole.VOLUNTEER, d), 10))
                .build();
    }

    // ── KPI + Yönetici özetleri ───────────────────────────────────────────────

    private List<String> buildKpis(ReportModules.TeamModule t, ReportModules.DamageModule dmg,
                                   ReportModules.ResourceModule r, ReportModules.StockModule s,
                                   ReportModules.VolunteerModule v) {
        List<String> kpis = new ArrayList<>();
        kpis.add(String.format("Görev tamamlama oranı: %%%.0f (%d/%d).",
                percent(t.completedTasks(), t.totalTasks()), t.completedTasks(), t.totalTasks()));
        kpis.add(String.format("Kaynak talebi karşılama oranı: %%%.0f.", r.fulfillmentRate()));
        if (s.criticalItems() + s.outOfStockItems() > 0) {
            kpis.add(String.format("Kritik/tükenmiş stok kalemi: %d.", s.criticalItems() + s.outOfStockItems()));
        }
        if (dmg.criticalRisk() > 0) {
            kpis.add(String.format("Kritik (yıkık) hasar bildirimi: %d.", dmg.criticalRisk()));
        }
        kpis.add(String.format("Aktif gönüllü: %d / %d.", v.active(), v.total()));
        return kpis;
    }

    private String buildComprehensiveSummary(ReportType type, Scope s, ReportModules.TeamModule t,
                                             ReportModules.DamageModule dmg, ReportModules.ResourceModule r,
                                             ReportModules.StockModule st, ReportModules.VolunteerModule v) {
        boolean noData = t.totalTasks() == 0 && dmg.total() == 0 && r.total() == 0
                && st.totalItems() == 0 && v.total() == 0;
        if (noData) {
            return "Seçilen kapsam ve dönem için yeterli operasyonel veri bulunamadı.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scopeWord(s)).append(" genelinde ");
        sb.append(String.format("toplam %d görevin %d'i tamamlanmış, %d'i devam etmektedir. ",
                t.totalTasks(), t.completedTasks(), t.inProgressTasks()));
        if (t.shortage() > 0) {
            sb.append(String.format("Aktif operasyonlar için yaklaşık %d kişilik ek personel ihtiyacı bulunmaktadır. ", t.shortage()));
        }
        sb.append(String.format("Kaynak taleplerinin %%%.0f'i karşılanmıştır. ", r.fulfillmentRate()));
        long criticalStock = st.criticalItems() + st.outOfStockItems();
        sb.append(criticalStock == 0
                ? "Kritik stok riski görülmemektedir. "
                : String.format("%d stok kaleminde kritik seviye mevcuttur; tedarik planlanmalıdır. ", criticalStock));
        if (dmg.coordinatorApproved() < dmg.total()) {
            sb.append(String.format("%d hasar bildiriminin %d'i koordinatör onaylıdır; kalanlar takip edilmelidir. ",
                    dmg.total(), dmg.coordinatorApproved()));
        }
        sb.append(String.format("Kapsamda %d gönüllünün %d'i aktiftir.", v.total(), v.active()));
        return sb.toString().trim();
    }

    private String buildOperationSummary(Scope s, ReportModules.TeamModule t) {
        if (t.totalTasks() == 0 && t.totalTeams() == 0) {
            return "Seçilen kapsamda ekip/operasyon verisi bulunamadı.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s genelinde toplam %d ekip bulunmaktadır. %d ekip aktif görevde, %d ekip müsait durumdadır. ",
                scopeWord(s), t.totalTeams(), t.activeTeams(), t.availableTeams()));
        sb.append(String.format("Toplam %d görevin %d'i tamamlanmış, %d'i devam etmektedir. ",
                t.totalTasks(), t.completedTasks(), t.inProgressTasks()));
        if (t.shortage() > 0) {
            sb.append(String.format("Mevcut operasyon yoğunluğuna göre yaklaşık %d kişilik ek ekip ihtiyacı bulunmaktadır.", t.shortage()));
        } else {
            sb.append("Mevcut personel kapasitesi aktif operasyonlar için yeterli görünmektedir.");
        }
        return sb.toString().trim();
    }

    private String buildResourceSummary(Scope s, ReportModules.ResourceModule r) {
        if (r.total() == 0) return "Seçilen kapsamda kaynak talebi verisi bulunamadı.";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s genelinde toplam %d kaynak talebi bulunmaktadır. %d talep karşılanmış (%%%.0f), %d talep beklemededir. ",
                scopeWord(s), r.total(), r.fulfilled(), r.fulfillmentRate(), r.pending()));
        if (r.criticalPending() > 0) {
            sb.append(String.format("%d adet kritik öncelikli talep halen açıktır; ivedilikle ele alınmalıdır. ", r.criticalPending()));
        }
        r.topCategories().stream().findFirst().ifPresent(c ->
                sb.append(String.format("En çok talep edilen kategori \"%s\"dır.", c.label())));
        return sb.toString().trim();
    }

    private String buildStockSummary(Scope s, ReportModules.StockModule st) {
        if (st.totalItems() == 0) return "Seçilen kapsamda stok verisi bulunamadı.";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s genelinde %d stok kaleminde toplam %d birim bulunmaktadır. ",
                scopeWord(s), st.totalItems(), st.totalQuantity()));
        long critical = st.criticalItems() + st.outOfStockItems();
        sb.append(critical == 0
                ? "Kritik veya tükenmiş kalem bulunmamaktadır. "
                : String.format("%d kalem kritik/tükenmiş seviyededir. ", critical));
        if (st.estimatedDaysRemaining() != null) {
            sb.append(String.format("Mevcut tüketim hızına göre tahmini %d günlük stok bulunmaktadır.", st.estimatedDaysRemaining()));
        }
        return sb.toString().trim();
    }

    private String buildDamageSummary(Scope s, ReportModules.DamageModule dmg) {
        if (dmg.total() == 0) return "Seçilen kapsamda hasar bildirimi verisi bulunamadı.";
        return String.format(
                "Seçilen bölgede toplam %d hasar bildirimi bulunmaktadır. Bunların %d tanesi sahada doğrulanmış, %d tanesi koordinatör tarafından onaylanmıştır. %d hasar kaydı için halen ekip ihtiyacı bulunmaktadır.",
                dmg.total(), dmg.fieldVerified(), dmg.coordinatorApproved(), dmg.teamNeeded());
    }

    private String buildVolunteerSummary(Scope s, ReportModules.VolunteerModule v) {
        if (v.total() == 0) return "Seçilen kapsamda gönüllü verisi bulunamadı.";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s genelinde %d kayıtlı gönüllü bulunmaktadır; %d tanesi aktiftir. ",
                scopeWord(s), v.total(), v.active()));
        sb.append(String.format("Dönem içinde %d yeni gönüllü kaydı yapılmıştır. ", v.newInPeriod()));
        v.neighborhoodDistribution().stream().findFirst().ifPresent(nm ->
                sb.append(String.format("En fazla gönüllü %s mahallesinde bulunmaktadır.", nm.neighborhoodName())));
        return sb.toString().trim();
    }

    private List<String> buildSupplyRecommendations(ReportModules.StockModule st) {
        List<String> recs = new ArrayList<>();
        if (st.outOfStockItems() > 0) {
            recs.add(String.format("%d tükenmiş kalem için acil tedarik gereklidir.", st.outOfStockItems()));
        }
        if (st.criticalItems() > 0) {
            recs.add(String.format("%d kritik seviyedeki kalem için tedarik planlanmalıdır.", st.criticalItems()));
        }
        if (st.estimatedDaysRemaining() != null && st.estimatedDaysRemaining() <= 3) {
            recs.add("Mevcut stok 3 gün veya daha az yeterlidir; öncelikli tedarik önerilir.");
        }
        if (recs.isEmpty()) recs.add("Acil tedarik ihtiyacı tespit edilmemiştir.");
        return recs;
    }

    // ── Kapsam çözümü (rol bazlı, BACKEND zorlamalı) ──────────────────────────

    private Scope resolveScope(UUID districtId, UUID neighborhoodId, UserPrincipal p) {
        return switch (p.getRole()) {
            // İlçe koordinatörü: ilçe kilitli (kendi ilçesi), mahalle serbest (kendi ilçesi içinde)
            case DISTRICT_COORDINATOR -> new Scope(p.getDistrictId(), neighborhoodId);
            // Mahalle koordinatörü: ilçe + mahalle kilitli
            case NEIGHBORHOOD_COORDINATOR -> new Scope(p.getDistrictId(), p.getNeighborhoodId());
            // ADMIN: serbest
            default -> new Scope(districtId, neighborhoodId);
        };
    }

    // ── Dönem çözümü (createdAt pencereli) ────────────────────────────────────

    private Period resolvePeriod(ReportType type, LocalDate date) {
        OffsetDateTime now = OffsetDateTime.now();
        switch (type) {
            case DAILY -> {
                LocalDate day = date != null ? date : LocalDate.now(TR);
                OffsetDateTime from = day.atStartOfDay(TR).toOffsetDateTime();
                OffsetDateTime to = day.plusDays(1).atStartOfDay(TR).toOffsetDateTime();
                return new Period(from, to, D_FMT.format(from));
            }
            case WEEKLY -> {
                if (date != null) {
                    OffsetDateTime from = date.atStartOfDay(TR).toOffsetDateTime();
                    OffsetDateTime to = date.plusDays(7).atStartOfDay(TR).toOffsetDateTime();
                    return new Period(from, to, D_FMT.format(from) + " - " + D_FMT.format(to.minusDays(1)));
                }
                OffsetDateTime from = now.minusDays(7);
                return new Period(from, now, D_FMT.format(from) + " - " + D_FMT.format(now) + " (son 7 gün)");
            }
            case MONTHLY -> {
                if (date != null) {
                    LocalDate firstDay = date.withDayOfMonth(1);
                    LocalDate nextMonth = firstDay.plusMonths(1);
                    OffsetDateTime from = firstDay.atStartOfDay(TR).toOffsetDateTime();
                    OffsetDateTime to = nextMonth.atStartOfDay(TR).toOffsetDateTime();
                    return new Period(from, to, D_FMT.format(from) + " - " + D_FMT.format(to.minusDays(1)));
                }
                OffsetDateTime from = now.minusDays(30);
                return new Period(from, now, D_FMT.format(from) + " - " + D_FMT.format(now) + " (son 30 gün)");
            }
            default -> {
                // Özel raporlar: son 30 gün (createdAt pencereli metrikler için)
                OffsetDateTime from = now.minusDays(30);
                return new Period(from, now, "Son 30 gün (" + D_FMT.format(from) + " - " + D_FMT.format(now) + ")");
            }
        }
    }

    // ── Sayım yardımcıları ────────────────────────────────────────────────────

    private long countRequests(UUID d, UUID n, ResourceRequestStatus status, RequestPriority priority) {
        Specification<com.afet.koordinasyon.domain.entity.ResourceRequest> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (d != null) ps.add(cb.equal(root.get("district").get("id"), d));
            if (n != null) ps.add(cb.equal(root.get("neighborhood").get("id"), n));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (priority != null) ps.add(cb.equal(root.get("priority"), priority));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return resourceRequestRepository.count(spec);
    }

    private long countDamage(UUID d, UUID n, DamageLevel level, VerificationStatus verification) {
        Specification<com.afet.koordinasyon.domain.entity.DamageAssessment> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (d != null) ps.add(cb.equal(root.get("district").get("id"), d));
            if (n != null) ps.add(cb.equal(root.get("neighborhood").get("id"), n));
            if (level != null) ps.add(cb.equal(root.get("damageLevel"), level));
            if (verification != null) ps.add(cb.equal(root.get("verificationStatus"), verification));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return damageAssessmentRepository.count(spec);
    }

    private List<Object[]> resourceTypeSummary(UUID d, UUID n) {
        if (n != null) return resourceRequestRepository.findResourceTypeSummaryByNeighborhood(n, ALL_REQ_STATUSES);
        if (d != null) return resourceRequestRepository.findResourceTypeSummaryByDistrict(d, ALL_REQ_STATUSES);
        return resourceRequestRepository.findResourceTypeSummary(ALL_REQ_STATUSES);
    }

    private List<ReportModules.NeighborhoodMetric> neighborhoodMetrics(List<Object[]> rows, int limit) {
        Map<UUID, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        if (map.isEmpty()) return List.of();
        Map<UUID, String> names = new LinkedHashMap<>();
        Map<UUID, String> districtNames = new LinkedHashMap<>();
        for (Neighborhood nb : neighborhoodRepository.findAllById(map.keySet())) {
            names.put(nb.getId(), nb.getName());
            if (nb.getDistrict() != null) districtNames.put(nb.getId(), nb.getDistrict().getName());
        }
        return map.entrySet().stream()
                .map(e -> new ReportModules.NeighborhoodMetric(
                        e.getKey().toString(),
                        names.getOrDefault(e.getKey(), "Bilinmeyen Mahalle"),
                        districtNames.get(e.getKey()),
                        e.getValue()))
                .sorted((a, b) -> Long.compare(b.value(), a.value()))
                .limit(limit)
                .toList();
    }

    // ── Meta / yardımcılar ────────────────────────────────────────────────────

    private ReportMeta buildMeta(ReportType type, Period period, Scope scope, UserPrincipal p) {
        String districtName = scope.districtId() != null
                ? districtRepository.findById(scope.districtId()).map(x -> x.getName()).orElse(null) : null;
        String neighborhoodName = scope.neighborhoodId() != null
                ? neighborhoodRepository.findById(scope.neighborhoodId()).map(x -> x.getName()).orElse(null) : null;
        String scopeLabel = scope.neighborhoodId() != null ? "Mahalle"
                : scope.districtId() != null ? "İlçe" : "Sistem Geneli";
        return ReportMeta.builder()
                .type(type.name())
                .typeLabel(type.getLabel())
                .periodLabel(period.label())
                .from(period.from().toString())
                .to(period.to().toString())
                .districtId(scope.districtId() != null ? scope.districtId().toString() : null)
                .districtName(districtName)
                .neighborhoodId(scope.neighborhoodId() != null ? scope.neighborhoodId().toString() : null)
                .neighborhoodName(neighborhoodName)
                .scopeLabel(scopeLabel)
                .generatedBy(p.getFirstName() + " " + p.getLastName())
                .role(roleLabel(p.getRole()))
                .generatedAt(DT_FMT.format(OffsetDateTime.now()))
                .build();
    }

    private String scopeWord(Scope s) {
        if (s.neighborhoodId() != null) return "Seçilen mahalle";
        if (s.districtId() != null) return "Seçilen ilçe";
        return "Sistem";
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case ADMIN -> "Yönetici";
            case DISTRICT_COORDINATOR -> "İlçe Koordinatörü";
            case NEIGHBORHOOD_COORDINATOR -> "Mahalle Koordinatörü";
            case VOLUNTEER -> "Gönüllü";
        };
    }

    private void audit(UserPrincipal p, ReportType type, Scope scope) {
        try {
            auditLogService.logUserAction(p, AuditActionType.REPORT_GENERATED, "REPORT", null,
                    "Rapor görüntülendi: " + type.getLabel(),
                    Map.of("type", type.name(),
                            "districtId", String.valueOf(scope.districtId()),
                            "neighborhoodId", String.valueOf(scope.neighborhoodId())));
        } catch (Exception e) {
            log.warn("Rapor audit log başarısız: {}", e.getMessage());
        }
    }

    private static double percent(long part, long whole) {
        if (whole <= 0) return 0.0;
        return Math.round((part * 1000.0 / whole)) / 10.0;
    }

    private Long minutesOrNull(Double seconds) {
        if (seconds == null || seconds <= 0) return null;
        return Math.round(seconds / 60.0);
    }

    private record Scope(UUID districtId, UUID neighborhoodId) {}

    private record Period(OffsetDateTime from, OffsetDateTime to, String label) {}
}
