package com.roofingcrm.api.v1.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.communication.CommunicationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommunicationLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommunicationLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommunicationLogService communicationLogService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        AuthenticatedUser authUser = new AuthenticatedUser(userId, "test@example.com");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(authUser, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void addForLead_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setChannel("CALL");
        request.setDirection("OUTBOUND");
        request.setSubject("Initial contact");
        request.setBody("Discussed roofing needs");

        CommunicationLogDto responseDto = new CommunicationLogDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setChannel("CALL");
        responseDto.setDirection("OUTBOUND");
        responseDto.setSubject("Initial contact");
        responseDto.setLeadId(leadId);
        responseDto.setOccurredAt(Instant.now());
        responseDto.setCreatedAt(Instant.now());

        when(communicationLogService.addForLead(eq(tenantId), eq(userId), eq(leadId), any(CreateCommunicationLogRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/leads/{leadId}/communications", leadId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel", is("CALL")))
                .andExpect(jsonPath("$.subject", is("Initial contact")));
    }

    @Test
    void addForJob_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setChannel("NOTE");
        request.setSubject("Site visit notes");
        request.setBody("Measured roof area");

        CommunicationLogDto responseDto = new CommunicationLogDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setChannel("NOTE");
        responseDto.setSubject("Site visit notes");
        responseDto.setJobId(jobId);
        responseDto.setOccurredAt(Instant.now());
        responseDto.setCreatedAt(Instant.now());

        when(communicationLogService.addForJob(eq(tenantId), eq(userId), eq(jobId), any(CreateCommunicationLogRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/jobs/{jobId}/communications", jobId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel", is("NOTE")))
                .andExpect(jsonPath("$.subject", is("Site visit notes")));
    }

    @Test
    void listForLead_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        CommunicationLogDto dto1 = new CommunicationLogDto();
        dto1.setId(UUID.randomUUID());
        dto1.setChannel("CALL");
        dto1.setSubject("First call");
        dto1.setLeadId(leadId);

        CommunicationLogDto dto2 = new CommunicationLogDto();
        dto2.setId(UUID.randomUUID());
        dto2.setChannel("EMAIL");
        dto2.setSubject("Follow up");
        dto2.setLeadId(leadId);

        when(communicationLogService.listForLead(eq(tenantId), eq(userId), eq(leadId)))
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/leads/{leadId}/communications", leadId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].channel", is("CALL")))
                .andExpect(jsonPath("$[1].channel", is("EMAIL")));
    }

    @Test
    void listForJob_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        CommunicationLogDto dto = new CommunicationLogDto();
        dto.setId(UUID.randomUUID());
        dto.setChannel("NOTE");
        dto.setSubject("Job update");
        dto.setJobId(jobId);

        when(communicationLogService.listForJob(eq(tenantId), eq(userId), eq(jobId)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/jobs/{jobId}/communications", jobId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subject", is("Job update")));
    }
}
