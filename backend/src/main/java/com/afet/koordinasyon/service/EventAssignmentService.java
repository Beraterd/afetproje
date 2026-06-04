package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Event;
import com.afet.koordinasyon.domain.entity.EventAssignment;
import com.afet.koordinasyon.domain.enums.EventInvitationStatus;
import com.afet.koordinasyon.domain.enums.EventStatus;
import com.afet.koordinasyon.dto.response.EventAssignmentResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.EventAssignmentRepository;
import com.afet.koordinasyon.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAssignmentService {

    private final EventAssignmentRepository assignmentRepository;
    private final EventRepository eventRepository;

    @Transactional
    public Map<String, String> acceptInvitation(String token) {
        EventAssignment assignment = assignmentRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("EventAssignment", "token", token));

        if (assignment.getStatus() == EventInvitationStatus.ACCEPTED) {
            return Map.of("message", "Bu davet zaten kabul edilmiş.", "status", "ALREADY_ACCEPTED");
        }
        if (assignment.getStatus() == EventInvitationStatus.DECLINED) {
            return Map.of("message", "Bu davet daha önce reddedilmiştir.", "status", "ALREADY_DECLINED");
        }
        if (assignment.getStatus() == EventInvitationStatus.CANCELLED) {
            return Map.of("message", "Bu görev artık aktif değil.", "status", "CANCELLED");
        }

        Event event = assignment.getEvent();
        if (event == null || event.getStatus() == EventStatus.CLOSED || event.getStatus() == EventStatus.COMPLETED) {
            return Map.of("message", "Bu görev artık aktif değil.", "status", "EVENT_INACTIVE");
        }

        if (assignment.getTokenExpiresAt() != null && assignment.getTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            return Map.of("message", "Bu davetin süresi dolmuş.", "status", "TOKEN_EXPIRED");
        }

        assignment.setStatus(EventInvitationStatus.ACCEPTED);
        assignment.setAcceptedAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);

        checkAndAutoCompleteEvent(event);

        return Map.of("message", "Görevi kabul ettiniz. Teşekkürler.", "status", "ACCEPTED",
                "eventTitle", event.getTitle());
    }

    @Transactional
    public Map<String, String> declineInvitation(String token) {
        EventAssignment assignment = assignmentRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("EventAssignment", "token", token));

        if (assignment.getStatus() == EventInvitationStatus.ACCEPTED) {
            return Map.of("message", "Bu davet zaten kabul edilmiş.", "status", "ALREADY_ACCEPTED");
        }
        if (assignment.getStatus() == EventInvitationStatus.DECLINED) {
            return Map.of("message", "Bu davet zaten reddedilmiştir.", "status", "ALREADY_DECLINED");
        }
        if (assignment.getStatus() == EventInvitationStatus.CANCELLED) {
            return Map.of("message", "Bu görev artık aktif değil.", "status", "CANCELLED");
        }

        if (assignment.getTokenExpiresAt() != null && assignment.getTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            return Map.of("message", "Bu davetin süresi dolmuş.", "status", "TOKEN_EXPIRED");
        }

        assignment.setStatus(EventInvitationStatus.DECLINED);
        assignment.setDeclinedAt(OffsetDateTime.now());
        assignmentRepository.save(assignment);

        return Map.of("message", "Davet reddedildi.", "status", "DECLINED");
    }

    @Transactional(readOnly = true)
    public List<EventAssignmentResponse> getEventAssignments(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }
        return assignmentRepository.findByEventIdWithUser(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void checkAndAutoCompleteEvent(Event event) {
        int acceptedCount = assignmentRepository.countByEventIdAndStatus(
                event.getId(), EventInvitationStatus.ACCEPTED);

        if (acceptedCount >= event.getRequiredPeople()) {
            event.setStatus(EventStatus.COMPLETED);
            event.setClosedAt(OffsetDateTime.now());
            eventRepository.save(event);
            log.info("Event auto-completed: eventId={}, acceptedCount={}, required={}",
                    event.getId(), acceptedCount, event.getRequiredPeople());
        }
    }

    private EventAssignmentResponse toResponse(EventAssignment ea) {
        return EventAssignmentResponse.builder()
                .id(ea.getId())
                .userId(ea.getUser().getId())
                .firstName(ea.getUser().getFirstName())
                .lastName(ea.getUser().getLastName())
                .email(ea.getUser().getEmail())
                .status(ea.getStatus().name())
                .invitedAt(ea.getInvitedAt())
                .acceptedAt(ea.getAcceptedAt())
                .declinedAt(ea.getDeclinedAt())
                .build();
    }
}
