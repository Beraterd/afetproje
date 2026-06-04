package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Team;
import com.afet.koordinasyon.domain.enums.TeamName;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.TeamMemberResponse;
import com.afet.koordinasyon.dto.response.TeamResponse;
import com.afet.koordinasyon.dto.response.TeamTypeResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.TeamMembershipRepository;
import com.afet.koordinasyon.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamCodeGeneratorService codeGenerator;

    public List<TeamTypeResponse> listTypes() {
        return Arrays.stream(TeamName.values())
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
                .map(this::toResponse)
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

    /**
     * Backfills team codes for teams that don't have one yet.
     * Also updates teams with old-format codes (e.g. "SAR-1", "FW-1") to new format.
     */
    @Transactional
    public int backfillTeamCodes() {
        List<Team> teams = teamRepository.findAll();
        int count = 0;

        for (Team team : teams) {
            String existing = team.getTeamCode();
            boolean needsUpdate = existing == null || existing.isBlank() || isOldFormat(existing);
            if (!needsUpdate) continue;

            String code = codeGenerator.generateTeamCode(team.getDistrict(), team.getName());

            // Handle unique constraint conflicts by incrementing
            int attempt = 0;
            while (teamRepository.existsByTeamCode(code) && attempt < 20) {
                // Force a fresh generation with next sequence
                code = codeGenerator.generateTeamCode(team.getDistrict(), team.getName());
                attempt++;
            }

            team.setTeamCode(code);
            teamRepository.save(team);
            log.info("Backfilled team code: old='{}' new='{}' teamId={}", existing, code, team.getId());
            count++;
        }
        return count;
    }

    /** Returns true if the code matches the old format (no team type segment). */
    private boolean isOldFormat(String code) {
        if (code == null) return true;
        // Old format examples: "SAR-1", "FW-1", "PEN-3" (2 parts separated by -)
        // New format: "PEN-TAH-1" (3 parts)
        return code.matches("^[A-Z]{2,5}-\\d+$");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TeamResponse toResponse(Team t) {
        return TeamResponse.builder()
                .id(t.getId())
                .name(t.getName().name())
                .teamCode(t.getTeamCode())
                .districtName(t.getDistrict() != null ? t.getDistrict().getName() : null)
                .coefficient(t.getCoefficient())
                .requiresDocument(t.getRequiresDocument() != null ? t.getRequiresDocument().name() : null)
                .description(t.getDescription())
                .activeMemberCount(teamMembershipRepository.countByTeamIdAndActiveTrue(t.getId()))
                .build();
    }

}
