package com.roofingcrm.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.GlobalExceptionHandler;
import com.roofingcrm.service.attachment.AttachmentUploadProperties;
import com.roofingcrm.api.publicapi.estimate.PublicEstimateController;
import com.roofingcrm.api.publicapi.estimate.PublicEstimateDto;
import com.roofingcrm.api.v1.auth.AuthController;
import com.roofingcrm.api.v1.auth.AuthResponse;
import com.roofingcrm.api.v1.auth.LoginRequest;
import com.roofingcrm.api.v1.auth.TenantSummaryDto;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.security.RefreshTokenProperties;
import com.roofingcrm.service.auth.AuthService;
import com.roofingcrm.service.estimate.PublicEstimateService;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests so only {@link HighRiskEndpointRateLimitFilter} runs (no Spring Security chain).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class HighRiskEndpointRateLimitFilterMvcTest {

    @Mock
    private AuthService authService;

    @Mock
    private PublicEstimateService publicEstimateService;

    private RefreshTokenProperties refreshTokenProperties;
    private RateLimitProperties rateLimitProperties;
    private MockMvc authMvc;
    private MockMvc publicMvc;

    @Mock
    private AttachmentUploadProperties attachmentUploadProperties;

    @BeforeEach
    void setUp() {
        refreshTokenProperties = new RefreshTokenProperties();
        refreshTokenProperties.setCookieName("rc_refresh_token");

        rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setLoginPerMinute(2);
        rateLimitProperties.setPublicResourceGetPerMinute(2);

        MinuteWindowRateLimiter limiter = new MinuteWindowRateLimiter();
        HighRiskEndpointRateLimitFilter filter =
                new HighRiskEndpointRateLimitFilter(rateLimitProperties, limiter, refreshTokenProperties);

        AuthController authController = new AuthController(authService, refreshTokenProperties);
        authMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler(attachmentUploadProperties))
                .addFilters(filter)
                .build();

        PublicEstimateController publicController = new PublicEstimateController(publicEstimateService);
        publicMvc = MockMvcBuilders.standaloneSetup(publicController).addFilters(filter).build();
    }

    @Test
    void login_withinLimit_succeeds() throws Exception {
        mockLoginSuccess();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret");

        for (int i = 0; i < 2; i++) {
            authMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
        verify(authService, times(2)).login(any());
    }

    @Test
    void login_exceedsLimit_returns429() throws Exception {
        mockLoginSuccess();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret");
        String json = new ObjectMapper().writeValueAsString(request);

        authMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
        authMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
        authMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", is(HighRiskEndpointRateLimitFilter.SAFE_LIMIT_MESSAGE)));

        verify(authService, times(2)).login(any());
    }

    @Test
    void login_badCredentials_sameMessageRegardlessOfUser_existenceNotLeaked() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResourceNotFoundException("Invalid email or password"));

        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("nope");

        authMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Invalid email or password")))
                .andExpect(jsonPath("$.message", not(containsString("exist"))))
                .andExpect(jsonPath("$.message", not(containsString("found"))));
    }

    @Test
    void login_separateEmailBuckets_underSameIp() throws Exception {
        mockLoginSuccess();

        LoginRequest a = new LoginRequest();
        a.setEmail("a@example.com");
        a.setPassword("p");
        LoginRequest b = new LoginRequest();
        b.setEmail("b@example.com");
        b.setPassword("p");
        ObjectMapper om = new ObjectMapper();

        authMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(a)))
                .andExpect(status().isOk());
        authMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(a)))
                .andExpect(status().isOk());
        authMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(a)))
                .andExpect(status().isTooManyRequests());

        authMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(b)))
                .andExpect(status().isOk());
    }

    @Test
    void publicEstimateGet_exceedsLimit_returns429() throws Exception {
        when(publicEstimateService.getByToken("tok1")).thenReturn(new PublicEstimateDto());

        publicMvc.perform(get("/api/public/estimates/tok1")).andExpect(status().isOk());
        publicMvc.perform(get("/api/public/estimates/tok1")).andExpect(status().isOk());
        publicMvc.perform(get("/api/public/estimates/tok1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", is(HighRiskEndpointRateLimitFilter.SAFE_LIMIT_MESSAGE)));
    }

    @Test
    void refresh_usesCookieInKey_differentCookiesAllowParallelBuckets() throws Exception {
        rateLimitProperties.setRefreshPerMinute(1);
        MinuteWindowRateLimiter limiter = new MinuteWindowRateLimiter();
        HighRiskEndpointRateLimitFilter filter =
                new HighRiskEndpointRateLimitFilter(rateLimitProperties, limiter, refreshTokenProperties);
        AuthController authController = new AuthController(authService, refreshTokenProperties);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler(attachmentUploadProperties))
                .addFilters(filter)
                .build();

        when(authService.refresh(any(), any(), any(), any()))
                .thenReturn(refreshOk());

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "token-one"))
                        .header("X-CSRF-Refresh", "csrf"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "token-one"))
                        .header("X-CSRF-Refresh", "csrf"))
                .andExpect(status().isTooManyRequests());

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("rc_refresh_token", "token-two"))
                        .header("X-CSRF-Refresh", "csrf"))
                .andExpect(status().isOk());
    }

    private void mockLoginSuccess() throws Exception {
        TenantSummaryDto tenantDto = new TenantSummaryDto();
        tenantDto.setTenantId(UUID.randomUUID());
        tenantDto.setTenantName("Co");
        tenantDto.setTenantSlug("co");
        tenantDto.setRole(UserRole.OWNER);

        AuthResponse response = new AuthResponse();
        response.setToken("jwt");
        response.setRefreshToken("refresh");
        response.setCsrfToken("csrf");
        response.setUserId(UUID.randomUUID());
        response.setEmail("user@example.com");
        response.setFullName("U");
        response.setTenants(List.of(tenantDto));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);
    }

    private static AuthResponse refreshOk() {
        TenantSummaryDto tenantDto = new TenantSummaryDto();
        tenantDto.setTenantId(UUID.randomUUID());
        tenantDto.setTenantName("Co");
        tenantDto.setTenantSlug("co");
        tenantDto.setRole(UserRole.OWNER);
        AuthResponse response = new AuthResponse();
        response.setToken("jwt");
        response.setCsrfToken("csrf");
        response.setUserId(UUID.randomUUID());
        response.setEmail("user@example.com");
        response.setFullName("U");
        response.setTenants(List.of(tenantDto));
        return response;
    }
}
