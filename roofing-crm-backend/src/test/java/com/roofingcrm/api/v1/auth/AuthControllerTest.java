package com.roofingcrm.api.v1.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void register_returnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setTenantName("Test Company");

        TenantSummaryDto tenantDto = new TenantSummaryDto();
        tenantDto.setTenantId(UUID.randomUUID());
        tenantDto.setTenantName("Test Company");
        tenantDto.setTenantSlug("test-company");
        tenantDto.setRole(UserRole.OWNER);

        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token-here");
        response.setUserId(UUID.randomUUID());
        response.setEmail("test@example.com");
        response.setFullName("Test User");
        response.setTenants(List.of(tenantDto));

        when(authService.registerOwner(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("jwt-token-here")))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.tenants[0].role", is("OWNER")));
    }

    @Test
    void login_returnsOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        TenantSummaryDto tenantDto = new TenantSummaryDto();
        tenantDto.setTenantId(UUID.randomUUID());
        tenantDto.setTenantName("Test Company");
        tenantDto.setTenantSlug("test-company");
        tenantDto.setRole(UserRole.OWNER);

        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token-here");
        response.setUserId(UUID.randomUUID());
        response.setEmail("test@example.com");
        response.setFullName("Test User");
        response.setTenants(List.of(tenantDto));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token-here")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setTenantName("Test Company");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingFields_returnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        // Missing password, fullName, tenantName

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());
    }
}
