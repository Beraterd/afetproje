package com.afet.koordinasyon.service.email;

import com.afet.koordinasyon.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationEventListener {

    private final EmailNotificationService emailNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEmailEvent event) {
        try {
            emailNotificationService.sendUserRegisteredEmail(
                    event.userId(), event.email(), event.firstName(), event.lastName());
        } catch (Exception e) {
            log.error("UserRegistered email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetEmailEvent event) {
        try {
            emailNotificationService.sendPasswordResetEmail(event.userId(), event.resetLink());
        } catch (Exception e) {
            log.error("PasswordReset email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEmailEvent event) {
        try {
            emailNotificationService.sendPasswordChangedEmail(event.userId());
        } catch (Exception e) {
            log.error("PasswordChanged email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDamageReportCreated(DamageReportCreatedEmailEvent event) {
        try {
            emailNotificationService.sendDamageReportCreated(event.damageAssessmentId(), event.reporterId());
        } catch (Exception e) {
            log.error("DamageReportCreated email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDamageReportStatusChanged(DamageReportStatusChangedEmailEvent event) {
        try {
            emailNotificationService.sendDamageReportStatusChanged(event.damageAssessmentId(), event.newStatus());
        } catch (Exception e) {
            log.error("DamageReportStatusChanged email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDamageReportFieldTeamAssigned(DamageReportFieldTeamAssignedEmailEvent event) {
        try {
            emailNotificationService.sendDamageReportFieldTeamAssigned(event.damageAssessmentId(), event.assigneeId());
        } catch (Exception e) {
            log.error("DamageReportFieldTeamAssigned email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVolunteerJoinedTask(VolunteerJoinedTaskEmailEvent event) {
        try {
            emailNotificationService.sendVolunteerJoinedTask(event.eventId(), event.userId());
        } catch (Exception e) {
            log.error("VolunteerJoinedTask email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEmailEvent event) {
        try {
            emailNotificationService.sendTaskCompleted(event.eventId());
        } catch (Exception e) {
            log.error("TaskCompleted email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResourceRequestCreated(ResourceRequestCreatedEmailEvent event) {
        try {
            emailNotificationService.sendResourceRequestCreated(event.resourceRequestId(), event.creatorId());
        } catch (Exception e) {
            log.error("ResourceRequestCreated email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResourceRequestStatusChanged(ResourceRequestStatusChangedEmailEvent event) {
        try {
            emailNotificationService.sendResourceRequestStatusChanged(event.resourceRequestId(), event.newStatus());
        } catch (Exception e) {
            log.error("ResourceRequestStatusChanged email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentApproved(DocumentApprovedEmailEvent event) {
        try {
            emailNotificationService.sendDocumentApproved(event.documentId());
        } catch (Exception e) {
            log.error("DocumentApproved email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentRejected(DocumentRejectedEmailEvent event) {
        try {
            emailNotificationService.sendDocumentRejected(event.documentId());
        } catch (Exception e) {
            log.error("DocumentRejected email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDistrictCoordinationCenterSet(DistrictCoordinationCenterSetEmailEvent event) {
        try {
            emailNotificationService.sendDistrictCoordinationCenterSet(event.districtId());
        } catch (Exception e) {
            log.error("DistrictCoordinationCenterSet email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNeighborhoodCoordinationCenterSet(NeighborhoodCoordinationCenterSetEmailEvent event) {
        try {
            emailNotificationService.sendNeighborhoodCoordinationCenterSet(event.neighborhoodId());
        } catch (Exception e) {
            log.error("NeighborhoodCoordinationCenterSet email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDistrictCoordinatorAssigned(DistrictCoordinatorAssignedEmailEvent event) {
        try {
            emailNotificationService.sendDistrictCoordinatorAssigned(event.districtId(), event.userId());
        } catch (Exception e) {
            log.error("DistrictCoordinatorAssigned email hatası: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNeighborhoodCoordinatorAssigned(NeighborhoodCoordinatorAssignedEmailEvent event) {
        try {
            emailNotificationService.sendNeighborhoodCoordinatorAssigned(event.neighborhoodId(), event.userId());
        } catch (Exception e) {
            log.error("NeighborhoodCoordinatorAssigned email hatası: {}", e.getMessage(), e);
        }
    }
}
