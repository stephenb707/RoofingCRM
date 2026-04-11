package com.roofingcrm.api.v1.dashboard;

import com.roofingcrm.api.GlobalExceptionHandler;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@SuppressWarnings("null")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

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
    void getSummary_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();

        DashboardSummaryDto body = new DashboardSummaryDto();
        body.setCustomerCount(3);
        body.setLeadCount(5);
        body.setJobCount(2);
        body.setEstimateCount(4);
        body.setInvoiceCount(1);
        body.setOpenTaskCount(7);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put(LeadStatus.NEW.name(), 2L);
        body.setLeadCountByStatus(byStatus);
        body.setJobsScheduledThisWeek(1);
        body.setUnscheduledJobsCount(2);
        body.setEstimatesSentCount(1);
        body.setUnpaidInvoiceCount(3);
        body.setActivePipelineLeadCount(4);
        body.setRecentLeads(List.of());
        body.setUpcomingJobs(List.of());
        body.setOpenTasks(List.of());

        when(dashboardService.getSummary(eq(tenantId), eq(userId))).thenReturn(body);

        mockMvc.perform(get("/api/v1/dashboard/summary").header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCount", is(3)))
                .andExpect(jsonPath("$.leadCount", is(5)))
                .andExpect(jsonPath("$.openTaskCount", is(7)))
                .andExpect(jsonPath("$.leadCountByStatus.NEW", is(2)));
    }
}
