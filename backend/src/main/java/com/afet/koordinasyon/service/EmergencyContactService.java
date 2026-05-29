package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.EmergencyContact;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.dto.response.EmergencyContactResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ConflictException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.EmergencyContactRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private static final int MAX_CONTACTS = 5;

    private final EmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> listContacts(UserPrincipal principal) {
        return emergencyContactRepository.findByOwnerIdOrderByCreatedAtAsc(principal.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmergencyContactResponse addContact(UUID contactUserId, UserPrincipal principal) {
        if (contactUserId.equals(principal.getId())) {
            throw new BusinessRuleException("Kendinizi yakın olarak ekleyemezsiniz");
        }

        long count = emergencyContactRepository.countByOwnerId(principal.getId());
        if (count >= MAX_CONTACTS) {
            throw new BusinessRuleException("En fazla " + MAX_CONTACTS + " yakın ekleyebilirsiniz");
        }

        if (emergencyContactRepository.existsByOwnerIdAndContactId(principal.getId(), contactUserId)) {
            throw new ConflictException("Bu kişi zaten yakın listenizde mevcut");
        }

        User owner = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        User contact = userRepository.findById(contactUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", contactUserId));

        if (!contact.isActive()) {
            throw new BusinessRuleException("Seçilen kullanıcı aktif değil");
        }

        EmergencyContact saved = emergencyContactRepository.save(
                EmergencyContact.builder().owner(owner).contact(contact).build());
        return toResponse(saved);
    }

    @Transactional
    public void removeContact(UUID contactUserId, UserPrincipal principal) {
        if (!emergencyContactRepository.existsByOwnerIdAndContactId(principal.getId(), contactUserId)) {
            throw new ResourceNotFoundException("EmergencyContact", "contactUserId", contactUserId);
        }
        emergencyContactRepository.deleteByOwnerIdAndContactId(principal.getId(), contactUserId);
    }

    private EmergencyContactResponse toResponse(EmergencyContact ec) {
        User c = ec.getContact();
        return EmergencyContactResponse.builder()
                .contactUserId(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .createdAt(ec.getCreatedAt())
                .build();
    }
}
