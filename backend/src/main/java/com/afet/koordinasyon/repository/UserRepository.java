package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:districtId IS NULL OR u.district.id = :districtId) AND " +
            "(:active IS NULL OR u.active = :active)")
    Page<User> findWithFilters(@Param("role") UserRole role,
            @Param("districtId") UUID districtId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    Page<User> findByRole(@Param("role") UserRole role, Pageable pageable);

    List<User> findByDistrictId(UUID districtId);

    List<User> findByNeighborhoodId(UUID neighborhoodId);

    List<User> findByActiveTrueAndEmailVerifiedTrue();

    /** AFAD deprem bildirimi: aktif+doğrulanmış tüm kullanıcılar; neighborhood LEFT JOIN FETCH
     *  ile yüklenir (lazy proxy hatası önlenir, mahallesi olmayanlar da dahil edilir). */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.neighborhood WHERE u.active = true AND u.emailVerified = true")
    List<User> findActiveVerifiedWithNeighborhood();

    /** Deprem bildirimi (rol filtreli): aktif+doğrulanmış kullanıcılardan belirli rollere sahip olanlar. */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.neighborhood WHERE u.active = true AND u.emailVerified = true AND u.role IN :roles")
    List<User> findActiveVerifiedWithRoles(@Param("roles") List<UserRole> roles);

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<User> searchByQuery(@Param("q") String query, Pageable pageable);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    /** Protected e-posta listesi dışındaki tüm kullanıcıları döner. */
    @Query("SELECT u FROM User u WHERE u.email NOT IN :emails")
    List<User> findByEmailNotIn(@Param("emails") List<String> emails);

    /** Deprem SMS bildirimi: aktif + mahalleye bağlı kullanıcılar; neighborhood JOIN FETCH. */
    @Query("SELECT u FROM User u JOIN FETCH u.neighborhood WHERE u.active = true AND u.emailVerified = true AND u.neighborhood IS NOT NULL")
    List<User> findSmsEligibleUsers();

    /** Admin kullanıcılar: sistem bildirimleri için. */
    @Query("SELECT u FROM User u WHERE u.role = com.afet.koordinasyon.domain.enums.UserRole.ADMIN AND u.active = true")
    List<User> findAdmins();
}
