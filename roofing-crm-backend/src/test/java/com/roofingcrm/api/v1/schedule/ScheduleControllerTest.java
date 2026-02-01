package com.roofingcrm.api.v1.schedule;

import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.job.JobService;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private TenantAccessService tenantAccessService;

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
    void listScheduleJobs_returnsOkWithJobs() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobDto dto = new JobDto();
        dto.setId(jobId);
        dto.setStatus(JobStatus.SCHEDULED);
        dto.setType(JobType.REPLACEMENT);
        dto.setScheduledStartDate(LocalDate.of(2026, 1, 3));
        dto.setScheduledEndDate(LocalDate.of(2026, 1, 5));
        dto.setCustomerFirstName("Jane");
        dto.setCustomerLastName("Doe");
        dto.setCreatedAt(Instant.now());
        dto.setUpdatedAt(Instant.now());

        when(jobService.listScheduleJobs(
                eq(tenantId), eq(userId),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 7)),
                isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/schedule/jobs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-07")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(jobId.toString())))
                .andExpect(jsonPath("$.content[0].status", is("SCHEDULED")))
                .andExpect(jsonPath("$.content[0].customerFirstName", is("Jane")));
    }

    @Test
    void listScheduleJobs_invalidDateRange_returnsBadRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/schedule/jobs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("startDate", "2026-01-07")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }
}
