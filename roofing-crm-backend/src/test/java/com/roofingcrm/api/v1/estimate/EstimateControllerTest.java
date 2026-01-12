package com.roofingcrm.api.v1.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.estimate.EstimateService;
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
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EstimateController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null") // Hamcrest matchers have nullable return types
class EstimateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EstimateService estimateService;

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
    void createEstimate_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        EstimateItemRequest item = new EstimateItemRequest();
        item.setName("Shingles");
        item.setQuantity(new BigDecimal("100"));
        item.setUnitPrice(new BigDecimal("25.00"));

        CreateEstimateRequest req = new CreateEstimateRequest();
        req.setTitle("Roof Estimate");
        req.setItems(List.of(item));

        EstimateItemDto itemDto = new EstimateItemDto();
        itemDto.setId(UUID.randomUUID());
        itemDto.setName("Shingles");
        itemDto.setQuantity(new BigDecimal("100"));
        itemDto.setUnitPrice(new BigDecimal("25.00"));

        EstimateDto responseDto = new EstimateDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setJobId(jobId);
        responseDto.setStatus(EstimateStatus.DRAFT);
        responseDto.setTitle("Roof Estimate");
        responseDto.setSubtotal(new BigDecimal("2500.00"));
        responseDto.setTotal(new BigDecimal("2500.00"));
        responseDto.setItems(List.of(itemDto));
        responseDto.setCreatedAt(Instant.now());

        when(estimateService.createEstimateForJob(eq(tenantId), eq(userId), eq(jobId), any(CreateEstimateRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/jobs/{jobId}/estimates", jobId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.title", is("Roof Estimate")))
                .andExpect(jsonPath("$.total", is(2500.00)));
    }

    @Test
    void getEstimate_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();

        EstimateDto dto = new EstimateDto();
        dto.setId(estimateId);
        dto.setStatus(EstimateStatus.DRAFT);
        dto.setTitle("Test Estimate");
        dto.setSubtotal(new BigDecimal("1000.00"));
        dto.setTotal(new BigDecimal("1000.00"));
        dto.setItems(List.of());
        dto.setCreatedAt(Instant.now());

        when(estimateService.getEstimate(eq(tenantId), eq(userId), eq(estimateId))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/estimates/{id}", estimateId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(estimateId.toString())))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void listEstimatesForJob_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        EstimateDto dto1 = new EstimateDto();
        dto1.setId(UUID.randomUUID());
        dto1.setJobId(jobId);
        dto1.setStatus(EstimateStatus.DRAFT);
        dto1.setItems(List.of());

        EstimateDto dto2 = new EstimateDto();
        dto2.setId(UUID.randomUUID());
        dto2.setJobId(jobId);
        dto2.setStatus(EstimateStatus.SENT);
        dto2.setItems(List.of());

        when(estimateService.listEstimatesForJob(eq(tenantId), eq(userId), eq(jobId))).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/jobs/{jobId}/estimates", jobId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void updateEstimateStatus_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID estimateId = UUID.randomUUID();

        UpdateEstimateStatusRequest req = new UpdateEstimateStatusRequest();
        req.setStatus(EstimateStatus.ACCEPTED);

        EstimateDto responseDto = new EstimateDto();
        responseDto.setId(estimateId);
        responseDto.setStatus(EstimateStatus.ACCEPTED);
        responseDto.setItems(List.of());
        responseDto.setCreatedAt(Instant.now());

        when(estimateService.updateEstimateStatus(eq(tenantId), eq(userId), eq(estimateId), eq(EstimateStatus.ACCEPTED)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/estimates/{id}/status", estimateId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }
}
