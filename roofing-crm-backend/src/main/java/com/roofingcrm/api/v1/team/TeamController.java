package com.roofingcrm.api.v1.team;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.team.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team")
@Validated
public class TeamController {

    private final TeamService teamService;

    @Autowired
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/members")
    public ResponseEntity<List<TeamMemberDto>> listMembers(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        List<TeamMemberDto> members = teamService.listMembers(tenantId, actorUserId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/invites")
    public ResponseEntity<List<TenantInviteDto>> listInvites(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        List<TenantInviteDto> invites = teamService.listInvites(tenantId, actorUserId);
        return ResponseEntity.ok(invites);
    }

    @PostMapping("/invites")
    public ResponseEntity<TenantInviteDto> createInvite(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateInviteRequest request) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        TenantInviteDto created = teamService.createInvite(tenantId, actorUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("inviteId") UUID inviteId) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        teamService.revokeInvite(tenantId, actorUserId, inviteId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/members/{userId}/role")
    public ResponseEntity<TeamMemberDto> updateMemberRole(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        TeamMemberDto updated = teamService.updateMemberRole(tenantId, actorUserId, userId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("userId") UUID userId) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        teamService.removeMember(tenantId, actorUserId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invites/accept")
    public ResponseEntity<AcceptInviteResponse> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request) {
        UUID actorUserId = SecurityUtils.getCurrentUserIdOrThrow();
        AcceptInviteResponse response = teamService.acceptInvite(actorUserId, request);
        return ResponseEntity.ok(response);
    }
}
