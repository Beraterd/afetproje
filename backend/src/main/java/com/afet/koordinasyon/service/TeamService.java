package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.TeamMemberResponse;
import com.afet.koordinasyon.dto.response.TeamResponse;
import com.afet.koordinasyon.dto.response.TeamTypeResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.TeamMembershipRepository;
import com.afet.koordinasyon.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    /**
     * Returns all active team types derived PURELY from the {@link TeamName} Java enum —
     * no Hibernate enum mapping, no DB row iteration, no activeMemberCount N+1 query.
     * Safe even when the teams table has legacy enum values (e.g. LOGISTICS) not present
     * in the current Java enum.
     *
     * LOGISTICS is intentionally excluded: it is a retired team type converted to OTHER
     * by V8 migration and should not be offered in the UI.
     */
    public List<TeamTypeResponse> listTypes() {
        return Arrays.stream(TeamName.values())
                // Exclude deprecated/retired types from the UI dropdown
                .filter(tn -> tn != TeamName.LOGISTICS)
                .map(tn -> TeamTypeResponse.builder()
                        .value(tn.name())
                        .label(tn.getLabel())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listAll() {
        return teamRepository.findAll().stream()
                .map(t -> TeamResponse.builder()
                        .id(t.getId())
                        .name(t.getName().name())
                        .coefficient(t.getCoefficient())
                        .requiresDocument(t.getRequiresDocument() != null ? t.getRequiresDocument().name() : null)
                        .description(t.getDescription())
                        .activeMemberCount(teamMembershipRepository.countByTeamIdAndActiveTrue(t.getId()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamMemberResponse> listMembers(UUID teamId, int page, int size) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team", "id", teamId);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "joinedAt"));
        var members = teamMembershipRepository.findByTeamIdAndActiveTrue(teamId, pageable);
        return PagedResponse.from(members.map(m -> TeamMemberResponse.builder()
                .userId(m.getUser().getId())
                .firstName(m.getUser().getFirstName())
                .lastName(m.getUser().getLastName())
                .email(m.getUser().getEmail())
                .phone(m.getUser().getPhone())
                .joinedAt(m.getJoinedAt())
                .build()));
    }
}
