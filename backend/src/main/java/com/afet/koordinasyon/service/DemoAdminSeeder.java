package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.BloodType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.DemoModeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * "Giriş Yapmadan Admin Olarak Siteyi Gez" özelliği için tek, sabit, salt okunur demo admin
 * hesabını (varsa) oluşturur. Bu hesap normal şifreyle giriş yapamaz (bkz. AuthService.login) —
 * yalnızca POST /api/auth/demo-login ile oturum açılabilir ve her yazma isteği
 * DemoModeWriteGuardFilter tarafından engellenir.
 */
@Component
@ConditionalOnProperty(name = "app.demo-admin-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DemoAdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Demo admin seed başladı ===");
        try {
            seed();
        } catch (Exception e) {
            log.error("Demo admin seed sırasında beklenmeyen hata — uygulama etkilenmez: {}", e.getMessage(), e);
        }
        log.info("=== Demo admin seed tamamlandı ===");
    }

    @Transactional
    protected void seed() {
        if (userRepository.existsByUsername(DemoModeConstants.DEMO_ADMIN_USERNAME)) {
            log.info("Demo admin zaten mevcut: {}", DemoModeConstants.DEMO_ADMIN_USERNAME);
            return;
        }

        List<District> districts = districtRepository.findAll();
        if (districts.isEmpty()) {
            log.warn("Demo admin seed: district tablosu boş, atlanıyor");
            return;
        }
        District district = districts.get(0);

        List<Neighborhood> neighborhoods = neighborhoodRepository.findByDistrictId(district.getId());
        if (neighborhoods.isEmpty()) {
            log.warn("Demo admin seed: '{}' ilçesinde mahalle bulunamadı, atlanıyor", district.getName());
            return;
        }
        Neighborhood neighborhood = neighborhoods.get(0);

        // Şifre ile normal girişte zaten reddedilir (AuthService.login); değer hiçbir yerde paylaşılmaz.
        String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User demoAdmin = User.builder()
                .username(DemoModeConstants.DEMO_ADMIN_USERNAME)
                .firstName("Demo")
                .lastName("Ziyaretçi")
                .email(DemoModeConstants.DEMO_ADMIN_EMAIL)
                .phone(DemoModeConstants.DEMO_ADMIN_PHONE)
                .bloodType(BloodType.O_POSITIVE)
                .address("Demo Ziyaretçi Modu")
                .passwordHash(unusablePassword)
                .role(UserRole.ADMIN)
                .demo(true)
                .district(district)
                .neighborhood(neighborhood)
                .build();

        userRepository.save(demoAdmin);
        log.info("Demo admin oluşturuldu: {}", DemoModeConstants.DEMO_ADMIN_USERNAME);
    }
}
