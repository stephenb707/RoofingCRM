package com.roofingcrm.api.v1.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.GlobalExceptionHandler;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.team.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@SuppressWarnings("null")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamService teamService;

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        AuthenticatedUser authUser = new AuthenticatedUser(userId, "test@example.com");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(authUser, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void listMembers_returnsOk() throws Exception {
        TeamMemberDto member = new TeamMemberDto();
        member.setUserId(userId);
        member.setEmail("test@example.com");
        member.setFullName("Test User");
        member.setRole(com.roofingcrm.domain.enums.UserRole.OWNER);

        when(teamService.listMembers(eq(tenantId), eq(userId))).thenReturn(List.of(member));

        mockMvc.perform(get("/api/v1/team/members")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email", org.hamcrest.Matchers.is("test@example.com")));
    }

    @Test
    void listInvites_returnsOk() throws Exception {
        TenantInviteDto invite = new TenantInviteDto();
        invite.setInviteId(UUID.randomUUID());
        invite.setEmail("invited@test.com");
        invite.setRole(com.roofingcrm.domain.enums.UserRole.SALES);
        invite.setToken(UUID.randomUUID());
        invite.setExpiresAt(Instant.now().plusSeconds(86400));
        invite.setCreatedAt(Instant.now());

        when(teamService.listInvites(eq(tenantId), eq(userId))).thenReturn(List.of(invite));

        mockMvc.perform(get("/api/v1/team/invites")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email", org.hamcrest.Matchers.is("invited@test.com")));
    }

    @Test
    void createInvite_returnsCreated() throws Exception {
        CreateInviteRequest request = new CreateInviteRequest();
        request.setEmail("new@test.com");
        request.setRole(com.roofingcrm.domain.enums.UserRole.ADMIN);

        TenantInviteDto created = new TenantInviteDto();
        created.setInviteId(UUID.randomUUID());
        created.setEmail("new@test.com");
        created.setRole(com.roofingcrm.domain.enums.UserRole.ADMIN);
        created.setToken(UUID.randomUUID());
        created.setExpiresAt(Instant.now().plusSeconds(604800));
        created.setCreatedAt(Instant.now());

        when(teamService.createInvite(eq(tenantId), eq(userId), any(CreateInviteRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/team/invites")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is("new@test.com")));
    }

    @Test
    void revokeInvite_returnsNoContent() throws Exception {
        UUID inviteId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/team/invites/{inviteId}", inviteId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateMemberRole_returnsOk() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setRole(com.roofingcrm.domain.enums.UserRole.SALES);

        TeamMemberDto updated = new TeamMemberDto();
        updated.setUserId(targetUserId);
        updated.setEmail("updated@test.com");
        updated.setRole(com.roofingcrm.domain.enums.UserRole.SALES);

        when(teamService.updateMemberRole(eq(tenantId), eq(userId), eq(targetUserId), any(UpdateMemberRoleRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/team/members/{userId}/role", targetUserId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("SALES")));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/team/members/{userId}", targetUserId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void acceptInvite_returnsOk() throws Exception {
        UUID token = UUID.randomUUID();
        AcceptInviteRequest request = new AcceptInviteRequest();
        request.setToken(token);

        AcceptInviteResponse response = new AcceptInviteResponse();
        response.setTenantId(tenantId);
        response.setTenantName("Test Tenant");
        response.setRole(com.roofingcrm.domain.enums.UserRole.SALES);

        when(teamService.acceptInvite(eq(userId), any(AcceptInviteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/team/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", org.hamcrest.Matchers.is(tenantId.toString())))
                .andExpect(jsonPath("$.tenantName", org.hamcrest.Matchers.is("Test Tenant")));
    }
}
