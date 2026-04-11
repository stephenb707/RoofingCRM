package com.roofingcrm.api.v1.report;

import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.report.AccountingJobsReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountingReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class AccountingReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountingJobsReportService accountingJobsReportService;

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
    void jobsXlsx_returns200_withXlsxContentTypeAndAttachment() throws Exception {
        byte[] body = new byte[] { 0x50, 0x4b, 0x03, 0x04 };
        when(accountingJobsReportService.generateAccountingJobsXlsx(eq(tenantId), eq(userId)))
                .thenReturn(body);

        mockMvc.perform(get("/api/v1/reports/accounting/jobs.xlsx")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"accounting-report-")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andExpect(content().bytes(body));

        verify(accountingJobsReportService).generateAccountingJobsXlsx(eq(tenantId), eq(userId));
    }
}
