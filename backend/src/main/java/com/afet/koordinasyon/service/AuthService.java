package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.LoginRequest;
import com.afet.koordinasyon.dto.request.RegisterRequest;
import com.afet.koordinasyon.dto.response.DistrictSummaryResponse;
import com.afet.koordinasyon.dto.response.LoginResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodSummaryResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ConflictException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.DemoModeConstants;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.email.UserRegisteredEmailEvent;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmailOrUsername(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Demo hesapları normal şifre akışıyla giriş yapamaz; yalnızca /api/auth/demo-login
        // üzerinden oturum açılabilir. Mesaj kasıtlı olarak genel "hatalı bilgi" mesajıyla aynı —
        // demo hesabının varlığı sızdırılmaz.
        if (principal.isDemo()) {
            throw new BadCredentialsException("Bad credentials");
        }

        String token = jwtTokenProvider.generateAccessToken(principal);
        auditLogService.logUserAction(
                principal.getId(),
                principal.getFirstName() + " " + principal.getLastName(),
                principal.getRole().name(),
                AuditActionType.USER_LOGIN, "USER", principal.getId(),
                principal.getFirstName() + " " + principal.getLastName() + " giriş yaptı",
                Map.of("email", principal.getEmail()));
        // Use getCurrentUser so the response includes coordinator-aware districtId/neighborhoodId
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                .user(getCurrentUser(principal.getId()))
                .build();
    }

    /**
     * Ziyaretçilerin kimlik doğrulaması yapmadan salt okunur "Admin Demo Modu" oturumu açmasını
     * sağlar. Sabit, seed edilmiş demo-admin hesabı dışında hiçbir kullanıcı için token üretmez.
     * Yazma istekleri DemoModeWriteGuardFilter tarafından backend seviyesinde engellenir.
     */
    @Transactional
    public LoginResponse demoLogin() {
        User demoUser = userRepository.findByUsername(DemoModeConstants.DEMO_ADMIN_USERNAME)
                .filter(User::isDemo)
                .orElseThrow(() -> new BusinessRuleException(
                        "Demo modu şu anda kullanılamıyor.", HttpStatus.SERVICE_UNAVAILABLE, "DEMO_MODE_UNAVAILABLE"));

        UserPrincipal principal = UserPrincipal.create(demoUser);
        String token = jwtTokenProvider.generateAccessToken(principal);

        auditLogService.logUserAction(
                principal.getId(),
                principal.getFirstName() + " " + principal.getLastName(),
                principal.getRole().name(),
                AuditActionType.USER_LOGIN, "USER", principal.getId(),
                "Ziyaretçi demo admin oturumu başlattı",
                Map.of("demo", true));

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                .user(getCurrentUser(principal.getId()))
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Bu e-posta adresi zaten kullanımda: " + request.getEmail());
        }
        // Telefon DB standardı E.164 (+905XXXXXXXXX); benzersizlik kontrolü de normalize değerle yapılır.
        String normalizedPhone = PhoneNumberUtil.toE164(request.getPhone());
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new ConflictException("Bu telefon numarası zaten kullanımda: " + normalizedPhone);
        }

        District district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", request.getDistrictId()));
        Neighborhood neighborhood = neighborhoodRepository.findById(request.getNeighborhoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", request.getNeighborhoodId()));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(normalizedPhone)
                .bloodType(request.getBloodType())
                .district(district)
                .neighborhood(neighborhood)
                .address(request.getAddress())
                .profession(request.getProfession())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        auditLogService.logSystemAction(AuditActionType.USER_CREATED, "USER", saved.getId(),
                saved.getFirstName() + " " + saved.getLastName() + " yeni kullanıcı kaydı oluşturuldu",
                Map.of("email", saved.getEmail(), "role", saved.getRole().name()));
        eventPublisher.publishEvent(new UserRegisteredEmailEvent(
                saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getLastName(), Instant.now()));
        return mapUserToResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return buildCoordinatorAwareResponse(userId, user);
    }

    /**
     * Builds a UserResponse where districtId / neighborhoodId reflect the coordinator's
     * ASSIGNMENT (via district.coordinator FK), not their residence.
     *
     * For DISTRICT_COORDINATOR:
     *   - districtId = assigned district (FK lookup); falls back to user.district if FK not set
     *
     * For NEIGHBORHOOD_COORDINATOR:
     *   - neighborhoodId = assigned neighborhood (FK lookup); falls back to user.neighborhood
     *   - districtId = district that contains the assigned neighborhood (for polygon lookup)
     *
     * For other roles: returns the base mapUserToResponse unchanged.
     */
    private UserResponse buildCoordinatorAwareResponse(UUID userId, User user) {
        UserResponse base = mapUserToResponse(user);

        switch (user.getRole()) {
            case DISTRICT_COORDINATOR -> {
                District assigned = districtRepository.findByCoordinatorId(userId).orElse(null);
                if (assigned != null) {
                    // FK assignment found — override residence with actual assignment
                    base.setDistrictId(assigned.getId());
                    base.setDistrict(DistrictSummaryResponse.builder()
                            .id(assigned.getId())
                            .name(assigned.getName())
                            .build());
                }
                // If no FK assignment, keep user.district (which may be manually set to the coordination district)
            }
            case NEIGHBORHOOD_COORDINATOR -> {
                Neighborhood assigned = neighborhoodRepository.findByCoordinatorId(userId).orElse(null);
                if (assigned != null) {
                    District d = assigned.getDistrict();
                    base.setNeighborhoodId(assigned.getId());
                    base.setNeighborhood(NeighborhoodSummaryResponse.builder()
                            .id(assigned.getId())
                            .name(assigned.getName())
                            .districtId(d != null ? d.getId() : null)
                            .districtName(d != null ? d.getName() : null)
                            .build());
                    // Set districtId to the containing district so frontend can load neighborhood polygons
                    if (d != null) {
                        base.setDistrictId(d.getId());
                        base.setDistrict(DistrictSummaryResponse.builder()
                                .id(d.getId())
                                .name(d.getName())
                                .build());
                    }
                }
                // If no FK assignment, keep user.neighborhood (may be manually set)
            }
            default -> { /* VOLUNTEER / ADMIN — use residence as-is */ }
        }

        return base;
    }

    public static UserResponse mapUserToResponse(User u) {
        District district = u.getDistrict();
        Neighborhood neighborhood = u.getNeighborhood();
        return UserResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .bloodType(u.getBloodType())
                .role(u.getRole())
                .active(u.isActive())
                .demo(u.isDemo())
                .district(district != null ? DistrictSummaryResponse.builder()
                        .id(district.getId()).name(district.getName()).build() : null)
                .neighborhood(neighborhood != null ? NeighborhoodSummaryResponse.builder()
                        .id(neighborhood.getId()).name(neighborhood.getName())
                        .districtId(district != null ? district.getId() : null)
                        .districtName(district != null ? district.getName() : null)
                        .build() : null)
                .districtId(district != null ? district.getId() : null)
                .neighborhoodId(neighborhood != null ? neighborhood.getId() : null)
                .address(u.getAddress())
                .profession(u.getProfession())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastKnownLatitude(u.getLastKnownLatitude())
                .lastKnownLongitude(u.getLastKnownLongitude())
                .lastKnownLocationAccuracy(u.getLastKnownLocationAccuracy())
                .locationPermissionStatus(u.getLocationPermissionStatus())
                .lastKnownLocationUpdatedAt(u.getLastKnownLocationUpdatedAt())
                .locationSource(u.getLocationSource())
                .build();
    }
}
