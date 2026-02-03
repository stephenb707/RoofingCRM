package com.roofingcrm.api.v1.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.invoice.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoiceService invoiceService;

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
    void createFromEstimate_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        CreateInvoiceFromEstimateRequest req = new CreateInvoiceFromEstimateRequest();
        req.setEstimateId(estimateId);

        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoiceId);
        dto.setInvoiceNumber("INV-1");
        dto.setStatus(InvoiceStatus.DRAFT);
        dto.setJobId(jobId);
        dto.setEstimateId(estimateId);
        dto.setTotal(new BigDecimal("5000"));
        dto.setIssuedAt(Instant.now());

        when(invoiceService.createFromEstimate(eq(tenantId), eq(userId), any(CreateInvoiceFromEstimateRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/invoices")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimateId\":\"" + estimateId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber", is("INV-1")))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        verify(invoiceService).createFromEstimate(eq(tenantId), eq(userId), any(CreateInvoiceFromEstimateRequest.class));
    }

    @Test
    void listInvoices_returnsPage() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        InvoiceDto dto = new InvoiceDto();
        dto.setId(UUID.randomUUID());
        dto.setInvoiceNumber("INV-1");
        dto.setStatus(InvoiceStatus.DRAFT);
        dto.setJobId(jobId);
        dto.setTotal(new BigDecimal("5000"));
        dto.setIssuedAt(Instant.now());
        dto.setItems(List.of());

        when(invoiceService.listInvoices(eq(tenantId), eq(userId), eq(jobId), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/invoices")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("jobId", jobId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].invoiceNumber", is("INV-1")));
    }

    @Test
    void listInvoicesForJob_returnsList() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        InvoiceDto dto = new InvoiceDto();
        dto.setId(UUID.randomUUID());
        dto.setInvoiceNumber("INV-1");
        dto.setStatus(InvoiceStatus.SENT);

        when(invoiceService.listInvoicesForJob(eq(tenantId), eq(userId), eq(jobId)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/invoices/job/{jobId}", jobId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].invoiceNumber", is("INV-1")));
    }

    @Test
    void getInvoice_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoiceId);
        dto.setInvoiceNumber("INV-1");
        dto.setStatus(InvoiceStatus.PAID);

        when(invoiceService.getInvoice(eq(tenantId), eq(userId), eq(invoiceId))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(invoiceId.toString())))
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    @Test
    void updateStatus_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        UpdateInvoiceStatusRequest req = new UpdateInvoiceStatusRequest();
        req.setStatus(InvoiceStatus.SENT);

        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoiceId);
        dto.setInvoiceNumber("INV-1");
        dto.setStatus(InvoiceStatus.SENT);

        when(invoiceService.updateStatus(eq(tenantId), eq(userId), eq(invoiceId), eq(InvoiceStatus.SENT)))
                .thenReturn(dto);

        mockMvc.perform(put("/api/v1/invoices/{id}/status", invoiceId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SENT")));

        verify(invoiceService).updateStatus(eq(tenantId), eq(userId), eq(invoiceId), eq(InvoiceStatus.SENT));
    }
}
