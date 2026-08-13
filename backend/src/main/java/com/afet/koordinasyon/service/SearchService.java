package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.response.GlobalSearchResponse;
import com.afet.koordinasyon.dto.response.SearchResultItem;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EventRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import com.afet.koordinasyon.repository.TeamRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uygulama çapında read-only global arama. Her entity tipi için mevcut role-scoping deseni
 * (EventService/DamageAssessmentService'teki district/neighborhood kapsam çözümü) birebir
 * tekrarlanır — arama, kullanıcının zaten normalde erişemediği kayıtları döndürmez.
 * Kategori başına sonuç sayısı sınırlıdır; büyük tablo taramaları yapılmaz.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int RESULTS_PER_CATEGORY = 5;

    private final EventRepository eventRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String query, UserPrincipal principal) {
        String q = query == null ? "" : query.trim();
        if (q.length() < MIN_QUERY_LENGTH) {
            return new GlobalSearchResponse(q, List.of());
        }

        UserRole role = principal.getRole();
        UUID districtId = null;
        UUID neighborhoodId = null;
        if (role == UserRole.DISTRICT_COORDINATOR) {
            districtId = districtRepository.findByCoordinatorId(principal.getId())
                    .map(District::getId).orElse(principal.getDistrictId());
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            neighborhoodId = neighborhoodRepository.findByCoordinatorId(principal.getId())
                    .map(Neighborhood::getId).orElse(principal.getNeighborhoodId());
            districtId = principal.getDistrictId();
        }
        // ADMIN ve VOLUNTEER: districtId/neighborhoodId null kalır (ilçe/mahalle kısıtı yok) —
        // mevcut Event/DamageAssessment/ResourceRequest listeleme endpoint'leriyle aynı davranış.

        Pageable limit = PageRequest.of(0, RESULTS_PER_CATEGORY);
        List<SearchResultItem> results = new ArrayList<>();

        eventRepository.searchByTitle(q, districtId, neighborhoodId, limit).forEach(e ->
                results.add(new SearchResultItem("EVENT", e.getId(), e.getTitle(),
                        e.getNeighborhood().getName(), e.getStatus().name())));

        damageAssessmentRepository.searchByAddress(q, districtId, neighborhoodId, limit).forEach(d ->
                results.add(new SearchResultItem("DAMAGE_ASSESSMENT", d.getId(), d.getAddress(),
                        d.getNeighborhood().getName() + " — " + d.getDamageLevel().name(),
                        d.getVerificationStatus().name())));

        teamRepository.searchByCode(q, districtId, limit).forEach(t ->
                results.add(new SearchResultItem("TEAM", t.getId(),
                        t.getTeamCode() != null ? t.getTeamCode() : t.getName().name(),
                        t.getName().getLabel(), null)));

        resourceRequestRepository.searchByTitle(q, districtId, neighborhoodId, limit).forEach(r ->
                results.add(new SearchResultItem("RESOURCE_REQUEST", r.getId(),
                        r.getTitle() != null ? r.getTitle() : r.getResourceType().name(),
                        r.getDistrict().getName(), r.getStatus().name())));

        // Kullanıcı sonuçları: VOLUNTEER için tamamen hariç (PII/yetki riski, spec'in açık talimatı).
        if (role != UserRole.VOLUNTEER) {
            userRepository.searchScoped(q, districtId, neighborhoodId, limit).forEach(u ->
                    results.add(new SearchResultItem("USER", u.getId(),
                            u.getFirstName() + " " + u.getLastName(),
                            u.getRole().name(), null)));
        }

        districtRepository.searchByName(q, limit).forEach(d ->
                results.add(new SearchResultItem("LOCATION", d.getId(), d.getName(), "İlçe", null)));

        neighborhoodRepository.searchByName(q, districtId, limit).forEach(n ->
                results.add(new SearchResultItem("LOCATION", n.getId(), n.getName(),
                        n.getDistrict().getName() + " — Mahalle", null)));

        return new GlobalSearchResponse(q, results);
    }
}
