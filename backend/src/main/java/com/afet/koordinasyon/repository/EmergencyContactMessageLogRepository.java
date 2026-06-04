package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EmergencyContactMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyContactMessageLogRepository extends JpaRepository<EmergencyContactMessageLog, UUID> {

    List<EmergencyContactMessageLog> findBySenderIdOrderByCreatedAtDesc(UUID senderId);
}
