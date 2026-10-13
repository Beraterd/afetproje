package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.TeamRecommendationRequest;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoTeamRecommendationTest {
    @Test
    void demoGeneratesPreviewWithoutPersistingOrSendingMail() {
        var recommendations = mock(TeamRecommendationRepository.class);
        var members = mock(TeamRecommendationMemberRepository.class);
        var users = mock(UserRepository.class);
        var districts = mock(DistrictRepository.class);
        var neighbors = mock(IstanbulDistrictNeighborService.class);
        var mail = mock(EmailNotificationService.class);
        var service = new TeamRecommendationService(recommendations, members, users, districts,
                mock(NeighborhoodRepository.class), mock(EventRepository.class), mock(EventVolunteerRepository.class),
                mock(EventAssignmentRepository.class), mail, neighbors, new ObjectMapper(), mock(AuditLogService.class));
        var user = new User();
        user.setId(UUID.randomUUID()); user.setFirstName("Demo"); user.setLastName("Admin"); user.setDemo(true);
        var district = District.builder().id(UUID.randomUUID()).name("Pendik").build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(districts.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighbors.getNeighbors("Pendik")).thenReturn(Set.of());
        when(users.findActiveVolunteersWithEmail(UserRole.VOLUNTEER)).thenReturn(List.of());
        var principal = new UserPrincipal(user.getId(), "Demo", "Admin", "demo@example.test", null,
                UserRole.ADMIN, null, null, true, true, List.of());
        var response = service.createRecommendation(
                new TeamRecommendationRequest("SEARCH_RESCUE", district.getId(), null, 2, "HIGH"), principal);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("DRAFT");
        verify(recommendations, never()).save(any());
        verify(members, never()).saveAll(any());
        verifyNoInteractions(mail);
    }
}
