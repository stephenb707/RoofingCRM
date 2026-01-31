package com.roofingcrm.api.v1.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.GlobalExceptionHandler;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.customer.CustomerService;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

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
    void createCustomer_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPrimaryPhone("555-1234");

        AddressDto addressDto = new AddressDto();
        addressDto.setLine1("123 Main St");
        request.setBillingAddress(addressDto);

        CustomerDto responseDto = new CustomerDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setFirstName("John");
        responseDto.setLastName("Doe");
        responseDto.setPrimaryPhone("555-1234");
        responseDto.setCreatedAt(Instant.now());

        when(customerService.createCustomer(eq(tenantId), eq(userId), any(CreateCustomerRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/customers")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));
    }

    @Test
    void createCustomer_missingFirstName_returnsBadRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();

        CreateCustomerRequest request = new CreateCustomerRequest();
        // firstName missing
        request.setLastName("Doe");
        request.setPrimaryPhone("555-1234");

        mockMvc.perform(post("/api/v1/customers")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCustomer_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        CustomerDto dto = new CustomerDto();
        dto.setId(customerId);
        dto.setFirstName("Jane");
        dto.setLastName("Smith");

        when(customerService.getCustomer(eq(tenantId), eq(userId), eq(customerId))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customerId.toString())))
                .andExpect(jsonPath("$.firstName", is("Jane")));
    }

    @Test
    void createCustomer_whenTenantAccessDenied_returnsForbidden() throws Exception {
        UUID tenantId = UUID.randomUUID();

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPrimaryPhone("555-1234");

        // Mock service to throw TenantAccessDeniedException
        when(customerService.createCustomer(eq(tenantId), any(), any(CreateCustomerRequest.class)))
                .thenThrow(new TenantAccessDeniedException("User does not have access to this tenant"));

        mockMvc.perform(post("/api/v1/customers")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
