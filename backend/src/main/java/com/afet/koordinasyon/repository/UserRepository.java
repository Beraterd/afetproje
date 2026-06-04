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

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<User> searchByQuery(@Param("q") String query, Pageable pageable);

    // ── Bakım / Purge sorguları ──────────────────────────────────────────────

    /** Protected e-posta listesi dışındaki tüm kullanıcıları döner. */
    @Query("SELECT u FROM User u WHERE u.email NOT IN :emails")
    List<User> findByEmailNotIn(@Param("emails") List<String> emails);

    /** Deprem SMS bildirimi: aktif + mahalleye bağlı kullanıcılar; neighborhood JOIN FETCH. */
    @Query("SELECT u FROM User u JOIN FETCH u.neighborhood WHERE u.active = true AND u.emailVerified = true AND u.neighborhood IS NOT NULL")
    List<User> findSmsEligibleUsers();

    /** Deprem WhatsApp bildirimi: aktif + doğrulanmış + telefon numarası olan kullanıcılar.
     *  neighborhood LEFT JOIN FETCH ile yüklenir (mahallesi olmayanlar da dahil; kısa link
     *  sayfası mahalleye göre içerik gösterir). */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.neighborhood " +
            "WHERE u.active = true AND u.emailVerified = true AND u.phone IS NOT NULL AND u.phone <> ''")
    List<User> findWhatsappEligibleUsers();

    /** Rapor: kapsamdaki toplam gönüllü sayısı (role parametre olarak verilir — pg enum cast'ından kaçınılır). */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role
              AND (:districtId IS NULL OR u.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR u.neighborhood.id = :neighborhoodId)
            """)
    long countVolunteersByRole(@Param("role") UserRole role,
                               @Param("districtId") UUID districtId,
                               @Param("neighborhoodId") UUID neighborhoodId);

    /** Rapor: kapsamdaki aktif gönüllü sayısı. */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role AND u.active = true
              AND (:districtId IS NULL OR u.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR u.neighborhood.id = :neighborhoodId)
            """)
    long countActiveVolunteersByRole(@Param("role") UserRole role,
                                     @Param("districtId") UUID districtId,
                                     @Param("neighborhoodId") UUID neighborhoodId);

    /** Rapor: kapsamda [from,to) aralığında YENİ kayıt olan gönüllü sayısı (createdAt). */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role AND u.createdAt >= :from AND u.createdAt < :to
              AND (:districtId IS NULL OR u.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR u.neighborhood.id = :neighborhoodId)
            """)
    long countNewVolunteersBetween(@Param("role") UserRole role,
                                   @Param("districtId") UUID districtId,
                                   @Param("neighborhoodId") UUID neighborhoodId,
                                   @Param("from") java.time.OffsetDateTime from,
                                   @Param("to") java.time.OffsetDateTime to);

    /** Rapor: kapsamda onaylı (aktif + e-posta doğrulanmış) gönüllü sayısı. */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role AND u.active = true AND u.emailVerified = true
              AND (:districtId IS NULL OR u.district.id = :districtId)
              AND (:neighborhoodId IS NULL OR u.neighborhood.id = :neighborhoodId)
            """)
    long countApprovedVolunteers(@Param("role") UserRole role,
                                 @Param("districtId") UUID districtId,
                                 @Param("neighborhoodId") UUID neighborhoodId);

    /** Rapor: mahalle bazında gönüllü dağılımı (ilçe kapsamında). [neighborhoodId, count] */
    @Query("""
            SELECT u.neighborhood.id, COUNT(u) FROM User u
            WHERE u.role = :role AND u.neighborhood IS NOT NULL
              AND (:districtId IS NULL OR u.district.id = :districtId)
            GROUP BY u.neighborhood.id
            """)
    List<Object[]> reportVolunteerCountByNeighborhood(@Param("role") UserRole role,
                                                      @Param("districtId") UUID districtId);

    /** Admin kullanıcılar: sistem bildirimleri için. */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true")
    List<User> findAdmins(@Param("role") UserRole role);

    /** Aktif VOLUNTEER kullanıcılar: AI öneri scoring için (emailVerified filtresi yok). */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.district LEFT JOIN FETCH u.neighborhood WHERE u.role = :role AND u.active = true AND (u.email IS NOT NULL AND u.email <> '')")
    List<User> findActiveVolunteersWithEmail(@Param("role") UserRole role);

    /** Tüm aktif kullanıcılar (rol filtresi yok): debug endpoint için. */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.district LEFT JOIN FETCH u.neighborhood WHERE u.active = true")
    List<User> findAllActiveAndVerified();
}
