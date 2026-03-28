package com.roofingcrm.api.v1.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.accounting.JobAccountingService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JobAccountingController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class JobAccountingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobAccountingService jobAccountingService;

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
    void getJobAccountingSummary_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobAccountingSummaryDto dto = new JobAccountingSummaryDto();
        dto.setAgreedAmount(new BigDecimal("12000.00"));
        dto.setInvoicedAmount(new BigDecimal("10000.00"));
        dto.setPaidAmount(new BigDecimal("8000.00"));
        dto.setTotalCosts(new BigDecimal("5500.00"));
        dto.setGrossProfit(new BigDecimal("2500.00"));
        dto.setMarginPercent(new BigDecimal("31.25"));
        dto.setCategoryTotals(Map.of(
                JobCostCategory.MATERIAL, new BigDecimal("3500.00"),
                JobCostCategory.LABOR, new BigDecimal("1200.00")
        ));
        dto.setHasAcceptedEstimate(true);

        when(jobAccountingService.getJobAccountingSummary(tenantId, userId, jobId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/jobs/{jobId}/accounting/summary", jobId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreedAmount", is(12000.00)))
                .andExpect(jsonPath("$.paidAmount", is(8000.00)))
                .andExpect(jsonPath("$.hasAcceptedEstimate", is(true)));
    }

    @Test
    void listJobCostEntries_returnsList() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobCostEntryDto dto = new JobCostEntryDto();
        dto.setId(UUID.randomUUID());
        dto.setJobId(jobId);
        dto.setCategory(JobCostCategory.MATERIAL);
        dto.setDescription("Shingles");
        dto.setAmount(new BigDecimal("3500.00"));
        dto.setIncurredAt(Instant.parse("2026-03-10T12:00:00Z"));

        when(jobAccountingService.listJobCostEntries(tenantId, userId, jobId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/jobs/{jobId}/costs", jobId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].description", is("Shingles")))
                .andExpect(jsonPath("$[0].category", is("MATERIAL")));
    }

    @Test
    void createJobCostEntry_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID costEntryId = UUID.randomUUID();

        JobCostEntryDto dto = new JobCostEntryDto();
        dto.setId(costEntryId);
        dto.setJobId(jobId);
        dto.setCategory(JobCostCategory.MATERIAL);
        dto.setDescription("Shingles");
        dto.setAmount(new BigDecimal("3500.00"));
        dto.setIncurredAt(Instant.parse("2026-03-10T12:00:00Z"));

        when(jobAccountingService.createJobCostEntry(eq(tenantId), eq(userId), eq(jobId), any(CreateJobCostEntryRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/jobs/{jobId}/costs", jobId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category":"MATERIAL",
                                  "description":"Shingles",
                                  "amount":3500.00,
                                  "incurredAt":"2026-03-10T12:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(costEntryId.toString())))
                .andExpect(jsonPath("$.category", is("MATERIAL")));

        verify(jobAccountingService).createJobCostEntry(eq(tenantId), eq(userId), eq(jobId), any(CreateJobCostEntryRequest.class));
    }

    @Test
    void updateJobCostEntry_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID costEntryId = UUID.randomUUID();

        UpdateJobCostEntryRequest request = new UpdateJobCostEntryRequest();
        request.setDescription("Updated labor");

        JobCostEntryDto dto = new JobCostEntryDto();
        dto.setId(costEntryId);
        dto.setJobId(jobId);
        dto.setCategory(JobCostCategory.LABOR);
        dto.setDescription("Updated labor");
        dto.setAmount(new BigDecimal("1200.00"));
        dto.setIncurredAt(Instant.parse("2026-03-11T12:00:00Z"));

        when(jobAccountingService.updateJobCostEntry(eq(tenantId), eq(userId), eq(jobId), eq(costEntryId), any(UpdateJobCostEntryRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(put("/api/v1/jobs/{jobId}/costs/{costEntryId}", jobId, costEntryId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated labor")));
    }

    @Test
    void deleteJobCostEntry_returnsNoContent() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID costEntryId = UUID.randomUUID();

        doNothing().when(jobAccountingService).deleteJobCostEntry(tenantId, userId, jobId, costEntryId);

        mockMvc.perform(delete("/api/v1/jobs/{jobId}/costs/{costEntryId}", jobId, costEntryId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        verify(jobAccountingService).deleteJobCostEntry(tenantId, userId, jobId, costEntryId);
    }
}
