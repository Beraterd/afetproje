package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.ResourceStock;
import com.afet.koordinasyon.domain.enums.*;
import com.afet.koordinasyon.dto.response.DailyReportResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rol bazlı (sistem / ilçe / mahalle) günlük yönetim raporu üretir.
 * Tüm istatistikler mevcut verilerden, hedefli sayım sorgularıyla hesaplanır.
 *
 * <p>§9 kapsamında yönetici seviyesinde karar destek bölümleri eklenmiştir:
 * Genel Durum Özeti, Risk Analizi, Operasyon Performansı, Mahalle Karşılaştırmaları,
 * Trend Analizi ve Yapay Zeka Destekli Yönetici Yorumu. Tüm bu bölümler mevcut
 * verilerden otomatik üretilir; harici servis çağrısı yapılmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ResourceRequestRepository resourceRequestRepository;
    private final ResourceStockRepository resourceStockRepository;
    private final ResourceStockMovementRepository movementRepository;
    private final ResourceStockService resourceStockService; // status/gün hesabı için
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final EventRepository eventRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final AssemblyAreaRepository assemblyAreaRepository;
    private final UserRepository userRepository;
    private final SystemNotificationRepository notificationRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final AuditLogService auditLogService;

    private static final ZoneId TR_ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(TR_ZONE);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(TR_ZONE);

    @Transactional(readOnly = true)
    public DailyReportResponse generateDaily(UUID districtId, UUID neighborhoodId, UserPrincipal principal) {
        Scope scope = resolveScope(districtId, neighborhoodId, principal);
        UUID d = scope.districtId();
        UUID n = scope.neighborhoodId();
        OffsetDateTime since24h = OffsetDateTime.now().minusHours(24);

        DailyReportResponse.ResourceStats resources = buildResourceStats(d, n);
        DailyReportResponse.StockStats stock = buildStockStats(d, n, since24h);
        DailyReportResponse.TaskStats tasks = buildTaskStats(d, n);
        DailyReportResponse.RequestStats requests = buildRequestStats(d, n);
        DailyReportResponse.DamageStats damage = buildDamageStats(d, n);
        DailyReportResponse.AssemblyStats assembly = buildAssemblyStats(d, n);
        DailyReportResponse.VolunteerStats volunteers = buildVolunteerStats(d, n);
        DailyReportResponse.CommunicationStats comm = buildCommunicationStats(d, n, since24h);

        List<String> kpis = buildKpis(resources, stock, tasks, requests, assembly, damage);

        // ── §9 Profesyonel karar destek bölümleri ─────────────────────────────
        DailyReportResponse.RiskAnalysis risk = buildRiskAnalysis(stock, requests, tasks, damage);
        DailyReportResponse.OperationPerformance performance = buildOperationPerformance(d, n, resources, tasks, stock);
        DailyReportResponse.TrendAnalysis trend = buildTrendAnalysis(d, n);
        List<DailyReportResponse.NeighborhoodComparison> comparisons = buildNeighborhoodComparisons(scope);
        DailyReportResponse.ExecutiveSummary summary =
                buildExecutiveSummary(resources, stock, tasks, requests, damage, assembly, risk);
        String aiCommentary = buildAiCommentary(scope, resources, stock, requests, tasks, damage, risk, trend, performance);

        auditReportGenerated(principal, scope, "GUNLUK");

        return DailyReportResponse.builder()
                .meta(buildMeta(scope, principal))
                .resources(resources)
                .stock(stock)
                .tasks(tasks)
                .requests(requests)
                .damage(damage)
                .assemblyAreas(assembly)
                .volunteers(volunteers)
                .communication(comm)
                .kpis(kpis)
                .executiveSummary(summary)
                .riskAnalysis(risk)
                .operationPerformance(performance)
                .neighborhoodComparisons(comparisons)
                .trendAnalysis(trend)
                .aiCommentary(aiCommentary)
                .build();
    }

    private void auditReportGenerated(UserPrincipal principal, Scope scope, String type) {
        try {
            auditLogService.logUserAction(principal, AuditActionType.REPORT_GENERATED, "REPORT", null,
                    "Yönetim raporu görüntülendi (" + type + ")",
                    Map.of("type", type,
                            "districtId", String.valueOf(scope.districtId()),
                            "neighborhoodId", String.valueOf(scope.neighborhoodId())));
        } catch (Exception e) {
            log.warn("Rapor audit log kaydı başarısız: {}", e.getMessage());
        }
    }

    // ── Bölümler ────────────────────────────────────────────────────────────

    private DailyReportResponse.ResourceStats buildResourceStats(UUID d, UUID n) {
        long total = countRequests(d, n, null, null);
        long fulfilled = countRequests(d, n, ResourceRequestStatus.FULFILLED, null);
        long pending = countRequests(d, n, ResourceRequestStatus.OPEN, null)
                + countRequests(d, n, ResourceRequestStatus.IN_PROGRESS, null);
        long rejected = countRequests(d, n, ResourceRequestStatus.CANCELLED, null);
        return DailyReportResponse.ResourceStats.builder()
                .total(total).fulfilled(fulfilled).pending(pending).rejected(rejected)
                .fulfillmentRate(percent(fulfilled, total))
                .build();
    }

    private DailyReportResponse.RequestStats buildRequestStats(UUID d, UUID n) {
        long total = countRequests(d, n, null, null);
        long open = countRequests(d, n, ResourceRequestStatus.OPEN, null)
                + countRequests(d, n, ResourceRequestStatus.IN_PROGRESS, null);
        long closed = countRequests(d, n, ResourceRequestStatus.FULFILLED, null)
                + countRequests(d, n, ResourceRequestStatus.CANCELLED, null);
        long urgent = countRequests(d, n, null, RequestPriority.HIGH);
        long critical = countRequests(d, n, null, RequestPriority.CRITICAL);
        return DailyReportResponse.RequestStats.builder()
                .total(total).open(open).closed(closed).urgent(urgent).critical(critical)
                .build();
    }

    private DailyReportResponse.StockStats buildStockStats(UUID d, UUID n, OffsetDateTime since) {
        List<ResourceStock> stocks = resourceStockRepository.search(d, n, null, false, "%");
        long critical = 0, outOfStock = 0;
        Map<ResourceType, Long> byCategory = new LinkedHashMap<>();
        for (ResourceStock s : stocks) {
            ResourceStockStatus st = resourceStockService.computeStatus(
                    s.getQuantity(), s.getCriticalThreshold(), s.getDailyUsageEstimate());
            if (st == ResourceStockStatus.OUT_OF_STOCK) outOfStock++;
            else if (st == ResourceStockStatus.CRITICAL) critical++;
            byCategory.merge(s.getCategory(), (long) s.getQuantity(), Long::sum);
        }
        List<DailyReportResponse.CategoryCount> dist = byCategory.entrySet().stream()
                .map(e -> new DailyReportResponse.CategoryCount(e.getKey().name(), e.getKey().getLabel(), e.getValue()))
                .toList();
        long consumed = movementRepository.sumConsumedSince(d, n, since);
        return DailyReportResponse.StockStats.builder()
                .totalItems(stocks.size()).criticalItems(critical).outOfStockItems(outOfStock)
                .last24hConsumed(consumed).categoryDistribution(dist)
                .build();
    }

    private DailyReportResponse.TaskStats buildTaskStats(UUID d, UUID n) {
        return DailyReportResponse.TaskStats.builder()
                .total(eventRepository.reportCountAllEvents(d, n))
                .completed(eventRepository.reportCountEventsByStatus(EventStatus.COMPLETED, d, n))
                .inProgress(eventRepository.reportCountEventsByStatus(EventStatus.IN_PROGRESS, d, n))
                .open(eventRepository.reportCountEventsByStatus(EventStatus.OPEN, d, n))
                .activeTeams(eventRepository.reportActiveTeams(EventStatus.COMPLETED, d, n))
                .build();
    }

    private DailyReportResponse.DamageStats buildDamageStats(UUID d, UUID n) {
        return DailyReportResponse.DamageStats.builder()
                .reported(countDamage(d, n, null, null))
                .verified(countDamage(d, n, null, VerificationStatus.KOORDINATOR_ONAYLADI))
                .pending(countDamage(d, n, null, VerificationStatus.INCELEME_GEREKIYOR))
                .light(countDamage(d, n, DamageLevel.LIGHT, null))
                .moderate(countDamage(d, n, DamageLevel.MODERATE, null))
                .heavy(countDamage(d, n, DamageLevel.HEAVY, null))
                .collapsed(countDamage(d, n, DamageLevel.COLLAPSED, null))
                .build();
    }

    private DailyReportResponse.AssemblyStats buildAssemblyStats(UUID d, UUID n) {
        Object[] row = assemblyAreaRepository.reportCountAndCapacity(d, n);
        // Aggregate tek satır döner; bazı sürümlerde Object[]{Object[]} sarmalı gelebilir.
        Object[] cells = (row.length == 1 && row[0] instanceof Object[] inner) ? inner : row;
        long count = ((Number) cells[0]).longValue();
        long capacity = cells[1] != null ? ((Number) cells[1]).longValue() : 0L;
        return DailyReportResponse.AssemblyStats.builder()
                .total(count).active(count).totalCapacity(capacity)
                .build();
    }

    private DailyReportResponse.VolunteerStats buildVolunteerStats(UUID d, UUID n) {
        return DailyReportResponse.VolunteerStats.builder()
                .total(userRepository.countVolunteersByRole(UserRole.VOLUNTEER, d, n))
                .active(userRepository.countActiveVolunteersByRole(UserRole.VOLUNTEER, d, n))
                .assigned(eventVolunteerRepository.reportAssignedVolunteers(d, n))
                .build();
    }

    private DailyReportResponse.CommunicationStats buildCommunicationStats(UUID d, UUID n, OffsetDateTime since) {
        long sent = notificationRepository.reportSentCount(d, n);
        long read = notificationRepository.reportReadCount(d, n);
        return DailyReportResponse.CommunicationStats.builder()
                .sent(sent).read(read).readRate(percent(read, sent))
                .last24h(notificationRepository.reportSentSince(d, n, since))
                .build();
    }

    // ── KPI üretimi ─────────────────────────────────────────────────────────

    private List<String> buildKpis(DailyReportResponse.ResourceStats r, DailyReportResponse.StockStats s,
                                   DailyReportResponse.TaskStats t, DailyReportResponse.RequestStats req,
                                   DailyReportResponse.AssemblyStats a, DailyReportResponse.DamageStats dmg) {
        List<String> kpis = new ArrayList<>();
        if (s.criticalItems() + s.outOfStockItems() > 0) {
            kpis.add(String.format("Kritik stok seviyesine yaklaşan %d ürün bulunmaktadır.",
                    s.criticalItems() + s.outOfStockItems()));
        }
        if (r.total() > 0 && r.pending() > 0) {
            kpis.add(String.format("Kaynak taleplerinin %%%.0f'i halen beklemektedir.",
                    percent(r.pending(), r.total())));
        }
        if (t.total() > 0) {
            kpis.add(String.format("Son durum itibarıyla görev tamamlama oranı %%%.0f olarak gerçekleşmiştir.",
                    percent(t.completed(), t.total())));
        }
        // En yoğun talep kategorisi (stok kategori dağılımına göre en çok kalemi olan kategori)
        s.categoryDistribution().stream()
                .max((x, y) -> Long.compare(x.quantity(), y.quantity()))
                .ifPresent(top -> kpis.add(String.format(
                        "En yüksek stok yoğunluğu \"%s\" kategorisindedir.", top.label())));
        if (req.critical() + req.urgent() > 0) {
            kpis.add(String.format("Öncelikli (acil/kritik) talep sayısı: %d.", req.critical() + req.urgent()));
        }
        if (dmg.reported() > 0) {
            kpis.add(String.format("Bildirilen %d hasardan %d tanesi doğrulanmıştır (doğrulama oranı %%%.0f).",
                    dmg.reported(), dmg.verified(), percent(dmg.verified(), dmg.reported())));
        }
        if (kpis.isEmpty()) {
            kpis.add("Seçilen kapsamda öne çıkan kritik bir gösterge bulunmamaktadır.");
        }
        return kpis;
    }

    // ── §9 Risk Analizi ────────────────────────────────────────────────────────

    /**
     * Otomatik risk puanı (0–100). Her faktör ağırlıklı puan üretir; toplam 100'de kırpılır.
     * Seviye eşikleri: <25 Düşük, <50 Orta, <75 Yüksek, ≥75 Kritik.
     */
    private DailyReportResponse.RiskAnalysis buildRiskAnalysis(
            DailyReportResponse.StockStats stock, DailyReportResponse.RequestStats requests,
            DailyReportResponse.TaskStats tasks, DailyReportResponse.DamageStats damage) {

        List<DailyReportResponse.RiskFactor> factors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        long criticalStock = stock.criticalItems() + stock.outOfStockItems();
        int stockPoints = (int) Math.min(25, criticalStock * 5);
        if (stockPoints > 0) {
            factors.add(new DailyReportResponse.RiskFactor("Kritik stok",
                    stockPoints, criticalStock + " kalem kritik/tükenmiş seviyede"));
            if (criticalStock >= 3) warnings.add("Kritik stok kalemleri için acil tedarik planlaması gereklidir.");
        }

        double pendingRatio = requests.total() > 0 ? (double) requests.open() / requests.total() : 0;
        int pendingPoints = (int) Math.min(20, Math.round(pendingRatio * 20));
        if (pendingPoints > 0) {
            factors.add(new DailyReportResponse.RiskFactor("Bekleyen talepler",
                    pendingPoints, "%" + Math.round(pendingRatio * 100) + " talep açık durumda"));
            if (pendingRatio >= 0.5) warnings.add("Açık talep oranı yüksek; talep karşılama hızlandırılmalıdır.");
        }

        long urgent = requests.urgent() + requests.critical();
        int urgentPoints = (int) Math.min(20, urgent * 4);
        if (urgentPoints > 0) {
            factors.add(new DailyReportResponse.RiskFactor("Acil talepler",
                    urgentPoints, urgent + " acil/kritik öncelikli talep"));
            if (urgent >= 3) warnings.add("Çok sayıda acil talep mevcut; öncelikli müdahale önerilir.");
        }

        long incompleteTasks = tasks.open() + tasks.inProgress();
        double incompleteRatio = tasks.total() > 0 ? (double) incompleteTasks / tasks.total() : 0;
        int taskPoints = (int) Math.min(15, Math.round(incompleteRatio * 15));
        if (taskPoints > 0) {
            factors.add(new DailyReportResponse.RiskFactor("Tamamlanmamış görevler",
                    taskPoints, incompleteTasks + " görev devam ediyor/açık"));
        }

        long severeDamage = damage.heavy() + damage.collapsed();
        double damageRatio = damage.reported() > 0 ? (double) severeDamage / damage.reported() : 0;
        int damagePoints = (int) Math.min(20, Math.round(damageRatio * 20) + Math.min(8, damage.collapsed() * 2));
        damagePoints = Math.min(20, damagePoints);
        if (damagePoints > 0) {
            factors.add(new DailyReportResponse.RiskFactor("Hasar yoğunluğu",
                    damagePoints, severeDamage + " ağır/yıkık yapı bildirimi"));
            if (damage.collapsed() > 0) warnings.add(damage.collapsed() + " yıkık yapı raporu bulunmaktadır; saha doğrulaması önceliklendirilmelidir.");
        }

        int total = Math.min(100, stockPoints + pendingPoints + urgentPoints + taskPoints + damagePoints);
        String level = total >= 75 ? "KRİTİK" : total >= 50 ? "YÜKSEK" : total >= 25 ? "ORTA" : "DÜŞÜK";
        if (factors.isEmpty()) {
            factors.add(new DailyReportResponse.RiskFactor("Genel durum", 0, "Öne çıkan risk faktörü tespit edilmedi"));
        }
        return DailyReportResponse.RiskAnalysis.builder()
                .score(total).level(level).factors(factors).warnings(warnings).build();
    }

    // ── §9 Operasyon Performansı ────────────────────────────────────────────────

    private DailyReportResponse.OperationPerformance buildOperationPerformance(
            UUID d, UUID n, DailyReportResponse.ResourceStats resources,
            DailyReportResponse.TaskStats tasks, DailyReportResponse.StockStats stock) {

        double actionable = resources.fulfilled() + resources.pending();
        Long avgResponse = minutesOrNull(eventRepository.reportAvgResolutionSeconds(d, n));
        Long avgResolution = minutesOrNull(resourceRequestRepository.reportAvgResolutionSeconds(d, n));

        return DailyReportResponse.OperationPerformance.builder()
                .taskCompletionRate(percent(tasks.completed(), tasks.total()))
                .requestFulfillmentRate(percent(resources.fulfilled(), resources.total()))
                .distributionPerformance(percent(resources.fulfilled(), (long) actionable))
                .avgResponseMinutes(avgResponse)
                .avgResolutionMinutes(avgResolution)
                .build();
    }

    private Long minutesOrNull(Double seconds) {
        if (seconds == null || seconds <= 0) return null;
        return Math.round(seconds / 60.0);
    }

    // ── §9 Trend Analizi (bugün / son 7 gün / son 30 gün) ──────────────────────

    private DailyReportResponse.TrendAnalysis buildTrendAnalysis(UUID d, UUID n) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfToday = LocalDate.now(TR_ZONE).atStartOfDay(TR_ZONE).toOffsetDateTime();
        OffsetDateTime d7 = now.minusDays(7);
        OffsetDateTime d14 = now.minusDays(14);
        OffsetDateTime d30 = now.minusDays(30);

        DailyReportResponse.TrendMetric requests = new DailyReportResponse.TrendMetric(
                resourceRequestRepository.reportCountCreatedBetween(d, n, startOfToday, now),
                resourceRequestRepository.reportCountCreatedBetween(d, n, d7, now),
                resourceRequestRepository.reportCountCreatedBetween(d, n, d30, now),
                changeRate(resourceRequestRepository.reportCountCreatedBetween(d, n, d7, now),
                           resourceRequestRepository.reportCountCreatedBetween(d, n, d14, d7)));

        DailyReportResponse.TrendMetric tasks = new DailyReportResponse.TrendMetric(
                eventRepository.reportCountCreatedBetween(d, n, startOfToday, now),
                eventRepository.reportCountCreatedBetween(d, n, d7, now),
                eventRepository.reportCountCreatedBetween(d, n, d30, now),
                changeRate(eventRepository.reportCountCreatedBetween(d, n, d7, now),
                           eventRepository.reportCountCreatedBetween(d, n, d14, d7)));

        DailyReportResponse.TrendMetric consumption = new DailyReportResponse.TrendMetric(
                movementRepository.sumConsumedBetween(d, n, startOfToday, now),
                movementRepository.sumConsumedBetween(d, n, d7, now),
                movementRepository.sumConsumedBetween(d, n, d30, now),
                changeRate(movementRepository.sumConsumedBetween(d, n, d7, now),
                           movementRepository.sumConsumedBetween(d, n, d14, d7)));

        DailyReportResponse.TrendMetric stockChange = new DailyReportResponse.TrendMetric(
                movementRepository.sumNetChangeBetween(d, n, startOfToday, now),
                movementRepository.sumNetChangeBetween(d, n, d7, now),
                movementRepository.sumNetChangeBetween(d, n, d30, now),
                changeRate(movementRepository.sumNetChangeBetween(d, n, d7, now),
                           movementRepository.sumNetChangeBetween(d, n, d14, d7)));

        return DailyReportResponse.TrendAnalysis.builder()
                .requests(requests).tasks(tasks)
                .resourceConsumption(consumption).stockChange(stockChange).build();
    }

    /** Önceki pencereye göre yüzdesel değişim; referans 0 ise null döner. */
    private Double changeRate(long current, long previous) {
        if (previous == 0) return null;
        return Math.round(((current - previous) * 1000.0 / Math.abs(previous))) / 10.0;
    }

    // ── §9 Mahalle Karşılaştırmaları ───────────────────────────────────────────

    private List<DailyReportResponse.NeighborhoodComparison> buildNeighborhoodComparisons(Scope scope) {
        // Mahalle kapsamında alt-kırılım yoktur.
        if (scope.neighborhoodId() != null) return List.of();
        UUID districtId = scope.districtId();

        Map<UUID, Long> requestMap = toCountMap(resourceRequestRepository.reportRequestCountByNeighborhood(districtId));
        Map<UUID, Long> damageMap = toCountMap(damageAssessmentRepository.reportDamageCountByNeighborhood(districtId));
        Map<UUID, Long> taskMap = toCountMap(eventRepository.reportEventCountByNeighborhood(districtId));
        Map<UUID, Long> usageMap = toCountMap(movementRepository.reportConsumedByNeighborhood(districtId));

        java.util.LinkedHashSet<UUID> ids = new java.util.LinkedHashSet<>();
        ids.addAll(requestMap.keySet());
        ids.addAll(damageMap.keySet());
        ids.addAll(taskMap.keySet());
        ids.addAll(usageMap.keySet());
        if (ids.isEmpty()) return List.of();

        Map<UUID, String> names = new LinkedHashMap<>();
        for (Neighborhood nb : neighborhoodRepository.findAllById(ids)) {
            names.put(nb.getId(), nb.getName());
        }

        List<DailyReportResponse.NeighborhoodComparison> list = new ArrayList<>();
        for (UUID id : ids) {
            long req = requestMap.getOrDefault(id, 0L);
            long dmg = damageMap.getOrDefault(id, 0L);
            long tsk = taskMap.getOrDefault(id, 0L);
            long usage = usageMap.getOrDefault(id, 0L);
            int risk = (int) Math.min(100, req * 3 + dmg * 4 + tsk * 2);
            list.add(DailyReportResponse.NeighborhoodComparison.builder()
                    .neighborhoodId(id.toString())
                    .neighborhoodName(names.getOrDefault(id, "Bilinmeyen Mahalle"))
                    .requestCount(req).resourceUsage(usage).taskCount(tsk)
                    .damageCount(dmg).riskScore(risk).build());
        }
        // En yüksek risk puanından düşüğe; ilk 15 mahalle.
        list.sort((a, b) -> Integer.compare(b.riskScore(), a.riskScore()));
        return list.size() > 15 ? list.subList(0, 15) : list;
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    // ── §9 Genel Durum Özeti ───────────────────────────────────────────────────

    private DailyReportResponse.ExecutiveSummary buildExecutiveSummary(
            DailyReportResponse.ResourceStats resources, DailyReportResponse.StockStats stock,
            DailyReportResponse.TaskStats tasks, DailyReportResponse.RequestStats requests,
            DailyReportResponse.DamageStats damage, DailyReportResponse.AssemblyStats assembly,
            DailyReportResponse.RiskAnalysis risk) {

        List<String> h = new ArrayList<>();

        h.add(switch (risk.level()) {
            case "DÜŞÜK" -> "İlçede operasyonlar normal seviyede devam etmektedir.";
            case "ORTA" -> "Operasyonlar kontrol altında olmakla birlikte izlenmesi gereken alanlar bulunmaktadır.";
            case "YÜKSEK" -> "Operasyonel yük yüksektir; bazı alanlarda yönetici müdahalesi önerilmektedir.";
            default -> "Kritik operasyonel durum söz konusudur; acil yönetici kararı gereklidir.";
        });

        if (resources.total() > 0) {
            h.add(resources.fulfillmentRate() >= 70
                    ? String.format("Kaynak taleplerinin büyük kısmı karşılanmıştır (%%%.0f).", resources.fulfillmentRate())
                    : String.format("Kaynak talepleri kısmen karşılanmıştır (%%%.0f); açık talepler takip edilmelidir.", resources.fulfillmentRate()));
        } else {
            h.add("Kapsam dahilinde kayıtlı kaynak talebi bulunmamaktadır.");
        }

        long criticalStock = stock.criticalItems() + stock.outOfStockItems();
        h.add(criticalStock == 0
                ? "Kritik stok riski bulunmamaktadır."
                : String.format("%d kalemde kritik stok riski mevcuttur; tedarik planlaması önerilir.", criticalStock));

        h.add(assembly.total() > 0
                ? String.format("Toplanma alanlarının doluluk/kapasite durumu kontrol altında tutulmaktadır (%d alan, %d kişi kapasite).",
                    assembly.total(), assembly.totalCapacity())
                : "Kapsamda aktif/onaylı toplanma alanı kaydı bulunmamaktadır; ivedilikle tanımlanmalıdır.");

        if (tasks.total() > 0) {
            h.add(String.format("Görev tamamlama oranı %%%.0f seviyesindedir.", percent(tasks.completed(), tasks.total())));
        }
        if (damage.collapsed() > 0) {
            h.add(String.format("%d yıkık yapı bildirimi öncelikli saha doğrulaması gerektirmektedir.", damage.collapsed()));
        }
        return DailyReportResponse.ExecutiveSummary.builder().highlights(h).build();
    }

    // ── §9 Yapay Zeka Destekli Yönetici Yorumu ─────────────────────────────────

    private String buildAiCommentary(
            Scope scope, DailyReportResponse.ResourceStats resources, DailyReportResponse.StockStats stock,
            DailyReportResponse.RequestStats requests, DailyReportResponse.TaskStats tasks,
            DailyReportResponse.DamageStats damage, DailyReportResponse.RiskAnalysis risk,
            DailyReportResponse.TrendAnalysis trend, DailyReportResponse.OperationPerformance perf) {

        StringBuilder sb = new StringBuilder();
        String scopeWord = scope.neighborhoodId() != null ? "mahalle" : scope.districtId() != null ? "ilçe" : "sistem";

        Double reqChange = trend.requests().changeRate();
        if (reqChange != null && reqChange != 0) {
            sb.append(String.format("Son 7 günlük veriler incelendiğinde kaynak taleplerinde %%%.0f %s görülmektedir. ",
                    Math.abs(reqChange), reqChange > 0 ? "artış" : "azalış"));
        } else {
            sb.append("Son 7 günlük kaynak talep hacminde belirgin bir değişim gözlenmemektedir. ");
        }

        // En yoğun stok kategorisi
        stock.categoryDistribution().stream()
                .max((x, y) -> Long.compare(x.quantity(), y.quantity()))
                .ifPresent(top -> sb.append(String.format("Stok dağılımında ağırlık \"%s\" kategorisindedir. ", top.label())));

        long criticalStock = stock.criticalItems() + stock.outOfStockItems();
        if (criticalStock == 0) {
            sb.append("Mevcut stok seviyeleri yeterli görünmektedir");
        } else {
            sb.append(String.format("Mevcut stok seviyeleri %d kalemde kritik seviyededir", criticalStock));
        }
        if (trend.resourceConsumption().changeRate() != null && trend.resourceConsumption().changeRate() > 15) {
            sb.append("; tüketim ivmesi göz önüne alındığında önümüzdeki 3 gün içerisinde ek tedarik planlaması önerilmektedir. ");
        } else {
            sb.append("; kısa vadede kritik bir tedarik baskısı öngörülmemektedir. ");
        }

        if (perf.avgResolutionMinutes() != null) {
            sb.append(String.format("Ortalama talep çözüm süresi yaklaşık %d dakikadır. ", perf.avgResolutionMinutes()));
        }

        sb.append(switch (risk.level()) {
            case "DÜŞÜK" -> "Genel risk seviyesi düşüktür; mevcut operasyon temposunun korunması yeterlidir.";
            case "ORTA" -> "Genel risk seviyesi ortadır; açık talep ve görevlerin yakından izlenmesi önerilir.";
            case "YÜKSEK" -> String.format("Genel risk seviyesi yüksektir (%d/100); kaynak ve ekip tahsisinin gözden geçirilmesi önerilir.", risk.score());
            default -> String.format("Genel risk seviyesi kritiktir (%d/100); üst yönetim düzeyinde acil koordinasyon gereklidir.", risk.score());
        });

        if (damage.reported() > 0 && damage.verified() < damage.reported()) {
            sb.append(String.format(" Ayrıca %d hasar bildiriminin %d tanesi henüz doğrulanmamıştır; saha ekipleri yönlendirilmelidir.",
                    damage.reported(), damage.reported() - damage.verified()));
        }
        return sb.toString().trim();
    }

    // ── Sayım yardımcıları (Specification) ─────────────────────────────────────

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

    // ── Kapsam çözümü + meta ───────────────────────────────────────────────────

    private Scope resolveScope(UUID districtId, UUID neighborhoodId, UserPrincipal principal) {
        return switch (principal.getRole()) {
            case DISTRICT_COORDINATOR -> new Scope(principal.getDistrictId(), neighborhoodId);
            case NEIGHBORHOOD_COORDINATOR -> new Scope(principal.getDistrictId(), principal.getNeighborhoodId());
            default -> new Scope(districtId, neighborhoodId); // ADMIN: serbest
        };
    }

    private DailyReportResponse.Meta buildMeta(Scope scope, UserPrincipal principal) {
        String districtName = scope.districtId() != null
                ? districtRepository.findById(scope.districtId()).map(x -> x.getName()).orElse(null) : null;
        String neighborhoodName = scope.neighborhoodId() != null
                ? neighborhoodRepository.findById(scope.neighborhoodId()).map(x -> x.getName()).orElse(null) : null;
        String scopeLabel = scope.neighborhoodId() != null ? "Mahalle Raporu"
                : scope.districtId() != null ? "İlçe Raporu" : "Sistem Geneli Raporu";
        OffsetDateTime now = OffsetDateTime.now();
        return DailyReportResponse.Meta.builder()
                .title("Afet Yönetim Günlük Faaliyet Raporu")
                .date(DATE_FMT.format(now))
                .time(TIME_FMT.format(now))
                .districtName(districtName)
                .neighborhoodName(neighborhoodName)
                .generatedBy(principal.getFirstName() + " " + principal.getLastName())
                .role(roleLabel(principal.getRole()))
                .scopeLabel(scopeLabel)
                .build();
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case ADMIN -> "Yönetici";
            case DISTRICT_COORDINATOR -> "İlçe Koordinatörü";
            case NEIGHBORHOOD_COORDINATOR -> "Mahalle Koordinatörü";
            case VOLUNTEER -> "Gönüllü";
        };
    }

    private static double percent(long part, long whole) {
        if (whole <= 0) return 0.0;
        return Math.round((part * 1000.0 / whole)) / 10.0;
    }

    private record Scope(UUID districtId, UUID neighborhoodId) {}
}
