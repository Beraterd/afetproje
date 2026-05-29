package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.domain.entity.EarthquakeSimulation;
import com.afet.koordinasyon.domain.entity.SimulationNotificationLog;
import com.afet.koordinasyon.domain.enums.NotificationStatus;
import com.afet.koordinasyon.domain.enums.SimulationEmailStatus;
import com.afet.koordinasyon.repository.AssemblyAreaRepository;
import com.afet.koordinasyon.repository.EarthquakeSimulationRepository;
import com.afet.koordinasyon.repository.SimulationNotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationEmailDispatcher {

    private final SimulationNotificationLogRepository notificationLogRepository;
    private final EarthquakeSimulationRepository simulationRepository;
    private final AssemblyAreaRepository assemblyAreaRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(SimulationEmailEvent event) {
        log.info("Starting async email dispatch for simulation {}", event.simulationId());

        List<SimulationNotificationLog> logs =
                notificationLogRepository.findBySimulationIdAndStatusUnpaged(
                        event.simulationId(), NotificationStatus.QUEUED);

        int sent = 0;
        int failed = 0;

        for (SimulationNotificationLog logEntry : logs) {
            var user = logEntry.getUser();

            List<AssemblyArea> areas;
            if (user.getNeighborhood() != null) {
                areas = assemblyAreaRepository.findVerifiedByNeighborhoodId(
                        user.getNeighborhood().getId());
            } else {
                areas = List.of();
            }

            List<EmailService.AssemblyAreaInfo> areaInfos = areas.stream()
                    .map(a -> new EmailService.AssemblyAreaInfo(a.getName(), a.getGoogleMapsUrl()))
                    .toList();

            String neighborhoodName = user.getNeighborhood() != null
                    ? user.getNeighborhood().getName()
                    : null;

            NotificationStatus status;
            String lastError = null;

            try {
                emailService.sendSimulationNotification(
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        neighborhoodName,
                        areaInfos
                );
                status = NotificationStatus.SENT;
                sent++;
                log.info("Simulation email sent to {}", user.getEmail());
            } catch (Exception e) {
                status = NotificationStatus.FAILED;
                failed++;
                lastError = e.getMessage();
                log.error("Failed to send simulation email to {}: {}", user.getEmail(), e.getMessage());
            }

            logEntry.setStatus(status);
            logEntry.setLastError(lastError);
            logEntry.setSentAt(status == NotificationStatus.SENT ? OffsetDateTime.now() : null);
            notificationLogRepository.save(logEntry);
        }

        SimulationEmailStatus finalStatus = (failed == 0)
                ? SimulationEmailStatus.COMPLETED
                : SimulationEmailStatus.PARTIAL_FAILURE;

        Optional<EarthquakeSimulation> simOpt =
                simulationRepository.findById(event.simulationId());

        if (simOpt.isPresent()) {
            EarthquakeSimulation sim = simOpt.get();
            sim.setEmailStatus(finalStatus);
            simulationRepository.save(sim);
        }

        log.info("Simulation {} email dispatch complete: sent={}, failed={}, status={}",
                event.simulationId(), sent, failed, finalStatus);
    }
}