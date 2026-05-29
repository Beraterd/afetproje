package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.DamageAssessmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DamageAssessmentAssignmentRepository extends JpaRepository<DamageAssessmentAssignment, UUID> {

    List<DamageAssessmentAssignment> findByDamageAssessmentIdAndActiveTrue(UUID damageAssessmentId);

    Optional<DamageAssessmentAssignment> findByIdAndDamageAssessmentIdAndActiveTrue(UUID id, UUID damageAssessmentId);

    boolean existsByDamageAssessmentIdAndUserIdAndActiveTrue(UUID damageAssessmentId, UUID userId);

    @Query("""
        SELECT a FROM DamageAssessmentAssignment a
        JOIN FETCH a.user u
        JOIN FETCH a.assignedBy
        JOIN FETCH a.damageAssessment da
        JOIN FETCH da.district
        JOIN FETCH da.neighborhood
        WHERE u.id = :userId AND a.active = true
        ORDER BY a.assignedAt DESC
        """)
    List<DamageAssessmentAssignment> findMyTasks(@Param("userId") UUID userId);

    @Query("""
        SELECT a FROM DamageAssessmentAssignment a
        JOIN FETCH a.user u
        JOIN FETCH a.assignedBy
        JOIN FETCH a.damageAssessment da
        JOIN FETCH da.district
        JOIN FETCH da.neighborhood
        WHERE a.id = :id
        """)
    Optional<DamageAssessmentAssignment> findByIdWithDetails(@Param("id") UUID id);
}
