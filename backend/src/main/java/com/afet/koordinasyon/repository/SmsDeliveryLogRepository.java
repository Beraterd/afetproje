package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.entity.SmsDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SmsDeliveryLogRepository extends JpaRepository<SmsDeliveryLog, UUID> {
}
