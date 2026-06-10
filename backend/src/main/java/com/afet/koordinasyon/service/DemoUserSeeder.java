package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.BloodType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo-users-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Demo kullanıcı seed başladı ===");

        try {
            seed();
        } catch (Exception e) {
            log.error("Demo kullanıcı seed sırasında beklenmeyen hata — uygulama etkilenmez: {}", e.getMessage(), e);
        }

        log.info("=== Demo kullanıcı seed tamamlandı ===");
    }

    @Transactional
    protected void seed() {
        List<District> districts = districtRepository.findAll();
        if (districts.isEmpty()) {
            log.warn("Demo seed: district tablosu boş, atlanıyor");
            return;
        }
        District district = districts.get(0);

        List<Neighborhood> neighborhoods = neighborhoodRepository.findByDistrictId(district.getId());
        if (neighborhoods.isEmpty()) {
            log.warn("Demo seed: '{}' ilçesinde mahalle bulunamadı, atlanıyor", district.getName());
            return;
        }
        Neighborhood neighborhood = neighborhoods.get(0);

        log.info("Demo seed: '{}' ilçesi / '{}' mahallesi kullanılıyor",
                district.getName(), neighborhood.getName());

        createIfAbsent("admin",   "Admin",   "Kullanıcı",    "admin@demo.local",   "+905551110001", "admin",   UserRole.ADMIN,                   district, neighborhood);
        createIfAbsent("ilce",    "İlçe",    "Koordinatörü", "ilce@demo.local",    "+905551110002", "ilce",    UserRole.DISTRICT_COORDINATOR,    district, neighborhood);
        createIfAbsent("mahalle", "Mahalle", "Koordinatörü", "mahalle@demo.local", "+905551110003", "mahalle", UserRole.NEIGHBORHOOD_COORDINATOR, district, neighborhood);
        createIfAbsent("gonul",   "Gönüllü", "Demo",         "gonul@demo.local",   "+905551110004", "gonul",   UserRole.VOLUNTEER,               district, neighborhood);
    }

    private void createIfAbsent(String username, String firstName, String lastName,
            String email, String phone, String password,
            UserRole role, District district, Neighborhood neighborhood) {
        try {
            if (userRepository.existsByUsername(username)) {
                log.info("Demo user already exists: {}", username);
                return;
            }

            log.info("Creating demo user: {}", username);

            User user = User.builder()
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phone(phone)
                    .bloodType(BloodType.O_POSITIVE)
                    .address("Demo Kullanıcı Adresi")
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .district(district)
                    .neighborhood(neighborhood)
                    .build();

            userRepository.save(user);
            log.info("Demo user created: {}", username);

        } catch (Exception e) {
            log.error("Demo user '{}' oluşturulamadı: {}", username, e.getMessage());
        }
    }
}
