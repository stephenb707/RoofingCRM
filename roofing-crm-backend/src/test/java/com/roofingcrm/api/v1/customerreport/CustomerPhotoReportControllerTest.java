package com.roofingcrm.api.v1.customerreport;

import com.roofingcrm.api.GlobalExceptionHandler;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.customerreport.CustomerPhotoReportPdfExport;
import com.roofingcrm.service.customerreport.CustomerPhotoReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerPhotoReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@SuppressWarnings("null")
class CustomerPhotoReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerPhotoReportService customerPhotoReportService;

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
    void attachmentCandidates_doesNotCollideWithReportIdPath() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerPhotoReportService.listAttachmentCandidates(eq(tenantId), eq(userId), eq(customerId), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/customer-photo-reports/attachment-candidates")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk());

        verify(customerPhotoReportService).listAttachmentCandidates(eq(tenantId), eq(userId), eq(customerId), isNull());
    }

    @Test
    void list_returnsSummaries() throws Exception {
        CustomerPhotoReportSummaryDto s = new CustomerPhotoReportSummaryDto();
        s.setId(UUID.randomUUID());
        s.setTitle("Inspection");
        when(customerPhotoReportService.list(eq(tenantId), eq(userId))).thenReturn(List.of(s));

        mockMvc.perform(get("/api/v1/customer-photo-reports")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Inspection"));
    }

    @Test
    void create_returnsDto() throws Exception {
        UpsertCustomerPhotoReportRequest req = new UpsertCustomerPhotoReportRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setTitle("Roof inspection");

        CustomerPhotoReportDto dto = new CustomerPhotoReportDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Roof inspection");
        when(customerPhotoReportService.create(eq(tenantId), eq(userId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/customer-photo-reports")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Roof inspection"));
    }

    @Test
    void create_validationFailsWithoutTitle() throws Exception {
        UpsertCustomerPhotoReportRequest req = new UpsertCustomerPhotoReportRequest();
        req.setCustomerId(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/customer-photo-reports")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_acceptsSectionsWithAttachmentIdsAndNullElements() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String body = String.format(
                "{\"customerId\":\"%s\",\"title\":\"Roof inspection\",\"jobId\":null,\"reportType\":null,\"summary\":null,"
                        + "\"sections\":[{\"title\":\"Front\",\"body\":\"Notes\",\"attachmentIds\":[\"%s\",null]}]}",
                customerId,
                photoId);

        CustomerPhotoReportDto dto = new CustomerPhotoReportDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Roof inspection");
        when(customerPhotoReportService.create(eq(tenantId), eq(userId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/customer-photo-reports")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Roof inspection"));
    }

    @Test
    void update_acceptsSamePayloadShape() throws Exception {
        UUID reportId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String body = String.format(
                "{\"customerId\":\"%s\",\"title\":\"Updated\",\"jobId\":null,\"sections\":[{\"title\":\"A\",\"attachmentIds\":[\"%s\"]}]}",
                customerId,
                photoId);

        CustomerPhotoReportDto dto = new CustomerPhotoReportDto();
        dto.setId(reportId);
        dto.setTitle("Updated");
        when(customerPhotoReportService.update(eq(tenantId), eq(userId), eq(reportId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(dto);

        mockMvc.perform(put("/api/v1/customer-photo-reports/" + reportId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void downloadPdf_returnsBytes() throws Exception {
        UUID reportId = UUID.randomUUID();
        when(customerPhotoReportService.exportPdf(eq(tenantId), eq(userId), eq(reportId)))
                .thenReturn(new CustomerPhotoReportPdfExport(new byte[] { '%', 'P', 'D', 'F' },
                        "customer-report-jane-doe-2026-04-11.pdf"));

        mockMvc.perform(get("/api/v1/customer-photo-reports/" + reportId + "/pdf")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition",
                        startsWith("attachment; filename=\"customer-report-jane-doe-2026-04-11.pdf\"")));

        verify(customerPhotoReportService).exportPdf(eq(tenantId), eq(userId), eq(reportId));
    }

    @Test
    void sendEmail_returnsSuccessPayload() throws Exception {
        UUID reportId = UUID.randomUUID();
        SendCustomerPhotoReportEmailResponse response = new SendCustomerPhotoReportEmailResponse();
        response.setSuccess(true);
        response.setSentAt(java.time.Instant.now());
        when(customerPhotoReportService.sendEmail(eq(tenantId), eq(userId), eq(reportId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/customer-photo-reports/" + reportId + "/send-email")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientEmail":"customer@example.com","recipientName":"Jane","subject":"Acme - Roof inspection","message":"Attached is your report."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sentAt").exists());

        verify(customerPhotoReportService).sendEmail(eq(tenantId), eq(userId), eq(reportId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID reportId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/customer-photo-reports/" + reportId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
        verify(customerPhotoReportService).archive(eq(tenantId), eq(userId), eq(reportId));
    }
}
