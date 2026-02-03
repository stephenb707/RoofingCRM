package com.roofingcrm.api.publicapi.estimate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.service.estimate.PublicEstimateService;
import com.roofingcrm.service.exception.EstimateLinkExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicEstimateController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class PublicEstimateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublicEstimateService publicEstimateService;

    @Test
    void getByToken_returnsEstimateWhenValid() throws Exception {
        String token = "abc123validtoken";
        PublicEstimateDto dto = new PublicEstimateDto();
        dto.setEstimateNumber("EST-1001");
        dto.setStatus(EstimateStatus.SENT);
        dto.setTitle("Roof Estimate");
        dto.setCustomerName("Jane Doe");
        dto.setSubtotal(new BigDecimal("5000.00"));
        dto.setTotal(new BigDecimal("5000.00"));
        dto.setItems(List.of());

        when(publicEstimateService.getByToken(eq(token))).thenReturn(dto);

        mockMvc.perform(get("/api/public/estimates/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimateNumber", is("EST-1001")))
                .andExpect(jsonPath("$.status", is("SENT")))
                .andExpect(jsonPath("$.customerName", is("Jane Doe")));
    }

    @Test
    void getByToken_returns410WhenExpired() throws Exception {
        String token = "expiredtoken";
        when(publicEstimateService.getByToken(eq(token)))
                .thenThrow(new EstimateLinkExpiredException("Link expired"));

        mockMvc.perform(get("/api/public/estimates/{token}", token))
                .andExpect(status().isGone());

        verify(publicEstimateService).getByToken(eq(token));
    }

    @Test
    void decide_returns409WhenAlreadyDecided() throws Exception {
        String token = "abc123token";
        when(publicEstimateService.decide(eq(token), any()))
                .thenThrow(new com.roofingcrm.service.exception.EstimateConflictException("Estimate has already been accepted or rejected"));

        String body = "{\"decision\":\"ACCEPTED\",\"signerName\":\"John\"}";

        mockMvc.perform(post("/api/public/estimates/{token}/decision", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void decide_updatesStatusAndReturnsSuccess() throws Exception {
        String token = "abc123validtoken";
        PublicEstimateDecisionRequest req = new PublicEstimateDecisionRequest();
        req.setDecision(EstimateStatus.ACCEPTED);
        req.setSignerName("John Homeowner");
        req.setSignerEmail("john@example.com");

        PublicEstimateDto dto = new PublicEstimateDto();
        dto.setEstimateNumber("EST-1001");
        dto.setStatus(EstimateStatus.ACCEPTED);
        dto.setTitle("Roof Estimate");
        dto.setItems(List.of());

        when(publicEstimateService.decide(eq(token), any(PublicEstimateDecisionRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/public/estimates/{token}/decision", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }
}
