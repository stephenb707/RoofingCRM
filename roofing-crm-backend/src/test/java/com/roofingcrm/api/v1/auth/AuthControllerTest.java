package com.roofingcrm.api.v1.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.security.RefreshTokenProperties;
import com.roofingcrm.service.auth.AuthService;
import com.roofingcrm.service.auth.AuthSessionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenProperties refreshTokenProperties;

    @BeforeEach
    void setUp() {
        when(refreshTokenProperties.getCookieName()).thenReturn("rc_refresh_token");
        when(refreshTokenProperties.getCookiePath()).thenReturn("/api/v1/auth");
        when(refreshTokenProperties.getSameSite()).thenReturn("Lax");
        when(refreshTokenProperties.getExpirationDays()).thenReturn(14L);
        when(refreshTokenProperties.isSecureCookie()).thenReturn(false);
    }

    @Test
    void register_returnsCreatedAndExposesCsrfTokenButNotRefreshToken() throws Exception {
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
        response.setRefreshToken("refresh-token");
        response.setCsrfToken("csrf-token-from-server");
        response.setUserId(UUID.randomUUID());
        response.setEmail("test@example.com");
        response.setFullName("Test User");
        response.setTenants(List.of(tenantDto));

        when(authService.registerOwner(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.token", is("jwt-token-here")))
                .andExpect(jsonPath("$.csrfToken", is("csrf-token-from-server")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.tenants[0].role", is("OWNER")));
    }

    @Test
    void login_returnsOkAndExposesCsrfToken() throws Exception {
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
        response.setRefreshToken("refresh-token");
        response.setCsrfToken("csrf-token-login");
        response.setUserId(UUID.randomUUID());
        response.setEmail("test@example.com");
        response.setFullName("Test User");
        response.setTenants(List.of(tenantDto));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=refresh-token")))
                .andExpect(jsonPath("$.token", is("jwt-token-here")))
                .andExpect(jsonPath("$.csrfToken", is("csrf-token-login")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    void registerWithInvite_returnsCreated() throws Exception {
        RegisterWithInviteRequest request = new RegisterWithInviteRequest();
        request.setEmail("invitee@example.com");
        request.setPassword("password123");
        request.setFullName("Invited User");
        request.setToken(UUID.randomUUID());

        TenantSummaryDto tenantDto = new TenantSummaryDto();
        tenantDto.setTenantId(UUID.randomUUID());
        tenantDto.setTenantName("Test Company");
        tenantDto.setTenantSlug("test-company");
        tenantDto.setRole(UserRole.SALES);

        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token-here");
        response.setUserId(UUID.randomUUID());
        response.setEmail("invitee@example.com");
        response.setFullName("Invited User");
        response.setTenants(List.of(tenantDto));

        when(authService.registerWithInvite(any(RegisterWithInviteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register-with-invite")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("invitee@example.com")))
                .andExpect(jsonPath("$.tenants[0].role", is("SALES")));
    }

    @Test
    void refresh_rotatesRefreshCookieAndReturnsAccessTokenAndCsrf() throws Exception {
        AuthResponse response = new AuthResponse();
        response.setToken("new-jwt");
        response.setRefreshToken("new-refresh");
        response.setCsrfToken("new-csrf");
        response.setUserId(UUID.randomUUID());
        response.setEmail("test@example.com");
        response.setFullName("Test User");
        response.setTenants(List.of());

        when(authService.refresh(eq("old-refresh"), eq("client-csrf"), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-CSRF-Refresh", "client-csrf")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=new-refresh")))
                .andExpect(jsonPath("$.token", is("new-jwt")))
                .andExpect(jsonPath("$.csrfToken", is("new-csrf")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void refresh_missingCsrfHeader_returns401AndClearsCookie() throws Exception {
        // No CSRF header => controller passes null csrf to the service, which throws.
        when(authService.refresh(eq("old-refresh"), isNull(), any(), any()))
                .thenThrow(new AuthSessionException("Missing refresh CSRF token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "old-refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void refresh_invalidCsrfHeader_returns401AndClearsCookie() throws Exception {
        when(authService.refresh(eq("old-refresh"), eq("wrong-csrf"), any(), any()))
                .thenThrow(new AuthSessionException("Invalid refresh CSRF token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-CSRF-Refresh", "wrong-csrf")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "old-refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logout_validCsrf_clearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-CSRF-Refresh", "client-csrf")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "old-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authService).logout("old-refresh", "client-csrf");
    }

    @Test
    void logout_missingCsrf_returns401AndClearsCookie() throws Exception {
        doThrow(new AuthSessionException("Missing refresh CSRF token"))
                .when(authService).logout(eq("old-refresh"), isNull());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "old-refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logout_invalidCsrf_returns401AndClearsCookie() throws Exception {
        doThrow(new AuthSessionException("Invalid refresh CSRF token"))
                .when(authService).logout(eq("old-refresh"), eq("wrong-csrf"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-CSRF-Refresh", "wrong-csrf")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "old-refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logout_missingRefreshCookie_returnsNoContentAndClearsCookie() throws Exception {
        // No refresh cookie at all; the service handles this as a no-op and returns 204.
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-CSRF-Refresh", "client-csrf"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("rc_refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authService).logout(isNull(), eq("client-csrf"));
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
