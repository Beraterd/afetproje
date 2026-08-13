package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.TeamRecommendation;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.RecommendationStatus;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.ApproveRecommendationRequest;
import com.afet.koordinasyon.repository.EventAssignmentRepository;
import com.afet.koordinasyon.repository.TeamRecommendationMemberRepository;
import com.afet.koordinasyon.repository.TeamRecommendationRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TeamRecommendationService onay/red akışının insan-onayı garantisini kapsar: AI önerisi
 * (createRecommendation) tek başına hiçbir EventAssignment yaratmaz — yalnızca açık bir
 * approve çağrısı, doğru role sahip bir kullanıcı tarafından yapıldığında mutasyon üretir ve
 * bu mutasyon audit'lenir. reject() hiçbir zaman EventAssignment yazmaz.
 */
@ExtendWith(MockitoExtension.class)
class TeamRecommendationServiceTest {

    @Mock private TeamRecommendationRepository recommendationRepository;
    @Mock private TeamRecommendationMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventAssignmentRepository eventAssignmentRepository;
    @Mock private AuditLogService auditLogService;

    private TeamRecommendationService service;

    private TeamRecommendation recommendation;
    private User approver;

    @BeforeEach
    void setUp() {
        District district = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        approver = new User();
        approver.setId(UUID.randomUUID());
        approver.setFirstName("Approver");
        approver.setLastName("User");
        recommendation = TeamRecommendation.builder()
                .id(UUID.randomUUID())
                .teamType(TeamName.SEARCH_RESCUE)
                .district(district)
                .requiredTeamSize(2)
                .priority("HIGH")
                .status(RecommendationStatus.DRAFT)
                .requestedBy(approver)
                .build();

        // Constructor injection: yalnızca approve/reject akışının kullandığı bağımlılıklar mock'lanıyor,
        // AI-üretim tarafının (Claude çağrısı, aday skorlama) mock'lanmasına bu testlerde gerek yok.
        service = new TeamRecommendationService(
                recommendationRepository, memberRepository, userRepository,
                null, null, null, null, eventAssignmentRepository,
                null, null, null, auditLogService);
    }

    private UserPrincipal principal(UserRole role) {
        return new UserPrincipal(UUID.randomUUID(), "Test", "User", "t@x.com",
                null, role, null, null, true, false, List.of());
    }

    @Test
    @DisplayName("VOLUNTEER önerinin kendisini onaylayamaz — AccessDeniedException fırlatılır, hiçbir şey kaydedilmez")
    void approveWithSelectedMembers_volunteer_throwsAndDoesNotMutate() {
        when(recommendationRepository.findById(recommendation.getId())).thenReturn(Optional.of(recommendation));
        UserPrincipal volunteer = principal(UserRole.VOLUNTEER);
        ApproveRecommendationRequest req = new ApproveRecommendationRequest(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.approveWithSelectedMembers(recommendation.getId(), req, volunteer))
                .isInstanceOf(AccessDeniedException.class);

        verify(eventAssignmentRepository, never()).save(any());
        verify(recommendationRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("DISTRICT_COORDINATOR onayladığında audit log AI kaynağını ve onaylayanı kaydeder")
    void approveWithSelectedMembers_districtCoordinator_writesAuditLog() {
        when(recommendationRepository.findById(recommendation.getId())).thenReturn(Optional.of(recommendation));
        when(userRepository.findById(any())).thenReturn(Optional.of(approver));
        when(memberRepository.findByRecommendationIdOrderByOrderIdAsc(recommendation.getId())).thenReturn(List.of());
        UserPrincipal dc = principal(UserRole.DISTRICT_COORDINATOR);
        ApproveRecommendationRequest req = new ApproveRecommendationRequest(List.of());

        service.approveWithSelectedMembers(recommendation.getId(), req, dc);

        verify(auditLogService).logUserAction(
                eq(dc),
                eq(AuditActionType.TEAM_ASSIGNED),
                eq("TeamRecommendation"),
                eq(recommendation.getId()),
                any(String.class),
                any());
    }

    @Test
    @DisplayName("reject() hiçbir zaman EventAssignment yazmaz — öneri sadece REJECTED durumuna geçer")
    void reject_neverWritesEventAssignment() {
        when(recommendationRepository.findById(recommendation.getId())).thenReturn(Optional.of(recommendation));
        when(memberRepository.findByRecommendationIdOrderByOrderIdAsc(recommendation.getId())).thenReturn(List.of());
        UserPrincipal admin = principal(UserRole.ADMIN);

        service.reject(recommendation.getId(), admin);

        verify(eventAssignmentRepository, never()).save(any());
    }
}
