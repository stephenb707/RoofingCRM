package com.roofingcrm.api.v1.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.service.lead.LeadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LeadController.class)
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeadService leadService;

    @Test
    void createLead_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateLeadRequest req = new CreateLeadRequest();
        NewLeadCustomerRequest newCustomer = new NewLeadCustomerRequest();
        newCustomer.setFirstName("Alice");
        newCustomer.setLastName("Roof");
        newCustomer.setPrimaryPhone("555-9999");
        req.setNewCustomer(newCustomer);

        AddressDto address = new AddressDto();
        address.setLine1("456 Roof St");
        req.setPropertyAddress(address);

        LeadDto responseDto = new LeadDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setStatus(LeadStatus.NEW);
        responseDto.setCreatedAt(Instant.now());

        when(leadService.createLead(eq(tenantId), eq(userId), any(CreateLeadRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/leads")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("NEW")));
    }

    @Test
    void getLead_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        LeadDto dto = new LeadDto();
        dto.setId(leadId);
        dto.setStatus(LeadStatus.NEW);
        dto.setCreatedAt(Instant.now());

        when(leadService.getLead(tenantId, leadId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/leads/{id}", leadId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(leadId.toString())))
                .andExpect(jsonPath("$.status", is("NEW")));
    }
}
