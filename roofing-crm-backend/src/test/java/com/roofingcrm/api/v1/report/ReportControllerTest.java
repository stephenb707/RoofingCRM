package com.roofingcrm.api.v1.report;

import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.report.ReportService;
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

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

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
    void leadsCsv_returns200_withCsvContentAndAttachmentHeaders() throws Exception {
        byte[] csvBytes = "leadId,customerName\nuuid-1,John Doe".getBytes();
        when(reportService.exportLeadsCsv(eq(userId), eq(tenantId), isNull(), isNull(), eq(2000)))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/v1/reports/leads.csv")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"leads-")))
                .andExpect(content().bytes(csvBytes));

        verify(reportService).exportLeadsCsv(eq(userId), eq(tenantId), isNull(), isNull(), eq(2000));
    }

    @Test
    void jobsCsv_returns200_withCsvContentAndAttachmentHeaders() throws Exception {
        byte[] csvBytes = "jobId,customerName\nuuid-1,Jane Smith".getBytes();
        when(reportService.exportJobsCsv(eq(userId), eq(tenantId), isNull(), eq(2000)))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/v1/reports/jobs.csv")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"jobs-")))
                .andExpect(content().bytes(csvBytes));

        verify(reportService).exportJobsCsv(eq(userId), eq(tenantId), isNull(), eq(2000));
    }

    @Test
    void leadsCsv_withLimit999999_capsAt5000() throws Exception {
        byte[] csvBytes = "header".getBytes();
        when(reportService.exportLeadsCsv(eq(userId), eq(tenantId), isNull(), isNull(), eq(5000)))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/v1/reports/leads.csv")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("limit", "999999"))
                .andExpect(status().isOk());

        verify(reportService).exportLeadsCsv(eq(userId), eq(tenantId), isNull(), isNull(), eq(5000));
    }

    @Test
    void leadsCsv_withStatusAndSource_passesFilters() throws Exception {
        byte[] csvBytes = "data".getBytes();
        when(reportService.exportLeadsCsv(eq(userId), eq(tenantId), eq(LeadStatus.NEW), eq(LeadSource.WEBSITE), eq(100)))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/v1/reports/leads.csv")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("status", "NEW")
                        .param("source", "WEBSITE")
                        .param("limit", "100"))
                .andExpect(status().isOk());

        verify(reportService).exportLeadsCsv(eq(userId), eq(tenantId), eq(LeadStatus.NEW), eq(LeadSource.WEBSITE), eq(100));
    }
}
