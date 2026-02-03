package com.roofingcrm.api.v1.job;

import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.job.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class JobScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

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
    void listSchedule_returnsOkWithJobs() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobDto dto = new JobDto();
        dto.setId(jobId);
        dto.setStatus(JobStatus.SCHEDULED);
        dto.setType(JobType.REPLACEMENT);
        dto.setScheduledStartDate(LocalDate.of(2026, 1, 15));
        dto.setScheduledEndDate(LocalDate.of(2026, 1, 17));
        dto.setCustomerFirstName("Jane");
        dto.setCustomerLastName("Doe");
        dto.setCreatedAt(Instant.now());
        dto.setUpdatedAt(Instant.now());

        when(jobService.listSchedule(
                eq(tenantId), eq(userId),
                eq(LocalDate.of(2026, 1, 13)), eq(LocalDate.of(2026, 1, 19)),
                isNull(), isNull(), eq(true)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/jobs/schedule")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "2026-01-13")
                        .param("to", "2026-01-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(jobId.toString())))
                .andExpect(jsonPath("$[0].status", is("SCHEDULED")))
                .andExpect(jsonPath("$[0].customerFirstName", is("Jane")));

        verify(jobService).listSchedule(
                eq(tenantId), eq(userId),
                eq(LocalDate.of(2026, 1, 13)), eq(LocalDate.of(2026, 1, 19)),
                isNull(), isNull(), eq(true));
    }

    @Test
    void listSchedule_withFilters_passesParamsCorrectly() throws Exception {
        UUID tenantId = UUID.randomUUID();

        when(jobService.listSchedule(
                eq(tenantId), eq(userId),
                eq(LocalDate.of(2026, 1, 6)), eq(LocalDate.of(2026, 1, 12)),
                eq(JobStatus.SCHEDULED), eq("Alpha"), eq(false)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/jobs/schedule")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "2026-01-06")
                        .param("to", "2026-01-12")
                        .param("status", "SCHEDULED")
                        .param("crewName", "Alpha")
                        .param("includeUnscheduled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(jobService).listSchedule(
                eq(tenantId), eq(userId),
                eq(LocalDate.of(2026, 1, 6)), eq(LocalDate.of(2026, 1, 12)),
                eq(JobStatus.SCHEDULED), eq("Alpha"), eq(false));
    }
}
