package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.EmailNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, UUID> {
}
