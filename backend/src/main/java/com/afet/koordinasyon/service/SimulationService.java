package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.EarthquakeSimulation;
import com.afet.koordinasyon.domain.entity.SimulationNotificationLog;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.NotificationStatus;
import com.afet.koordinasyon.domain.enums.SimulationEmailStatus;
import com.afet.koordinasyon.dto.request.CreateSimulationRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final EarthquakeSimulationRepository simulationRepository;
    private final SimulationNotificationLogRepository notificationLogRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SimulationCreatedResponse triggerSimulation(CreateSimulationRequest request, UserPrincipal principal) {
        District district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", request.getDistrictId()));

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        EarthquakeSimulation simulation = EarthquakeSimulation.builder()
                .district(district)
                .magnitude(request.getMagnitude())
                .createdBy(creator)
                .notes(request.getNotes())
                .emailStatus(SimulationEmailStatus.PROCESSING)
                .build();

        EarthquakeSimulation saved = simulationRepository.save(simulation);

        // Tüm aktif kullanıcılar için QUEUED notification log oluştur.
        // E-posta gönderimi transaction commit edildikten sonra arka planda
        // SimulationEmailDispatcher tarafından async olarak yapılır.
        List<User> allUsers = userRepository.findAll();
        int userCount = 0;
        for (User user : allUsers) {
            if (!user.isActive()) continue;
            notificationLogRepository.save(SimulationNotificationLog.builder()
                    .simulation(saved)
                    .user(user)
                    .emailAddress(user.getEmail())
                    .status(NotificationStatus.QUEUED)
                    .build());
            userCount++;
        }

        // Transaction commit sonrası async e-posta gönderimini tetikle.
        // @TransactionalEventListener(phase = AFTER_COMMIT) garantisi ile
        // SimulationEmailDispatcher.dispatch() ayrı thread'de çalışır.
        eventPublisher.publishEvent(new SimulationEmailEvent(saved.getId()));

        log.info("Simulation {} created for district {}. {} users queued for async email dispatch.",
                saved.getId(), district.getName(), userCount);

        return SimulationCreatedResponse.builder()
                .simulationId(saved.getId())
                .districtId(district.getId())
                .districtName(district.getName())
                .magnitude(saved.getMagnitude())
                .emailStatus(saved.getEmailStatus().name())
                .totalUsersToNotify(userCount)
                .triggeredAt(saved.getTriggeredAt())
                .build();
    }

    @Transactional(readOnly = true)
    public com.afet.koordinasyon.dto.response.ActiveSimulationResponse getActiveSimulation() {
        return simulationRepository.findTop1ByOrderByCreatedAtDesc()
                .map(sim -> com.afet.koordinasyon.dto.response.ActiveSimulationResponse.builder()
                        .id(sim.getId())
                        .districtName(sim.getDistrict().getName())
                        .magnitude(sim.getMagnitude())
                        .triggeredAt(sim.getTriggeredAt())
                        .build())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SimulationDetailResponse> listSimulations(int page, int size) {
        Page<EarthquakeSimulation> simPage = simulationRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "triggeredAt")));
        Page<SimulationDetailResponse> mapped = simPage.map(this::toDetailResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public SimulationDetailResponse getSimulation(UUID id) {
        EarthquakeSimulation sim = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation", "id", id));
        return toDetailResponse(sim);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SimulationLogResponse> getSimulationLogs(UUID simulationId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SimulationNotificationLog> logsPage;
        if (status != null && !status.isBlank()) {
            NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
            logsPage = notificationLogRepository.findBySimulationIdAndStatus(simulationId, notifStatus, pageable);
        } else {
            logsPage = notificationLogRepository.findBySimulationId(simulationId, pageable);
        }
        return PagedResponse.from(logsPage.map(this::toLogResponse));
    }

    private SimulationDetailResponse toDetailResponse(EarthquakeSimulation sim) {
        int totalSent = notificationLogRepository.countBySimulationIdAndStatus(sim.getId(), NotificationStatus.SENT);
        int totalFailed = notificationLogRepository.countBySimulationIdAndStatus(sim.getId(), NotificationStatus.FAILED);
        int totalQueued = notificationLogRepository.countBySimulationId(sim.getId());

        return SimulationDetailResponse.builder()
                .id(sim.getId())
                .district(DistrictSummaryResponse.builder()
                        .id(sim.getDistrict().getId())
                        .name(sim.getDistrict().getName())
                        .build())
                .magnitude(sim.getMagnitude())
                .notes(sim.getNotes())
                .emailStatus(sim.getEmailStatus().name())
                .totalQueued(totalQueued)
                .totalSent(totalSent)
                .totalFailed(totalFailed)
                .triggeredAt(sim.getTriggeredAt())
                .createdBy(UserSummaryResponse.builder()
                        .id(sim.getCreatedBy().getId())
                        .firstName(sim.getCreatedBy().getFirstName())
                        .lastName(sim.getCreatedBy().getLastName())
                        .email(sim.getCreatedBy().getEmail())
                        .build())
                .build();
    }

    private SimulationLogResponse toLogResponse(SimulationNotificationLog log) {
        return SimulationLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .emailAddress(log.getEmailAddress())
                .status(log.getStatus().name())
                .retryCount(log.getRetryCount())
                .lastError(log.getLastError())
                .sentAt(log.getSentAt())
                .build();
    }
}
