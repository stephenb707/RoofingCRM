package com.roofingcrm.api.v1.job;

import com.roofingcrm.api.GlobalExceptionHandler;
import com.roofingcrm.config.CorsProperties;
import com.roofingcrm.config.SecurityConfig;
import com.roofingcrm.config.SecurityErrorHandlers;
import com.roofingcrm.domain.enums.JobStatus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.service.job.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for schedule endpoint with real security filters.
 * Verifies that GET /api/v1/jobs/schedule returns 200 when authenticated
 * and returns JSON ApiErrorResponse (not empty) when access is denied.
 */
@WebMvcTest(controllers = JobController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorHandlers.class, JobScheduleSecurityIntegrationTest.TestCorsConfig.class})
@SuppressWarnings("null")
class JobScheduleSecurityIntegrationTest {

    private static final String VALID_TOKEN = "Bearer test-jwt-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private JwtService jwtService;

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        when(jwtService.parseToken("test-jwt-token"))
                .thenReturn(new AuthenticatedUser(userId, "test@example.com"));
    }

    @Test
    void listSchedule_withValidJwt_returns200() throws Exception {
        var dto = new JobDto();
        dto.setId(UUID.randomUUID());
        dto.setStatus(JobStatus.SCHEDULED);
        dto.setType(JobType.REPLACEMENT);
        dto.setScheduledStartDate(LocalDate.of(2026, 1, 15));
        dto.setScheduledEndDate(LocalDate.of(2026, 1, 17));
        dto.setCreatedAt(Instant.now());
        dto.setUpdatedAt(Instant.now());

        when(jobService.listSchedule(
                eq(tenantId), eq(userId),
                eq(LocalDate.of(2026, 1, 13)), eq(LocalDate.of(2026, 1, 19)),
                isNull(), isNull(), eq(true)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/jobs/schedule")
                        .header("Authorization", VALID_TOKEN)
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "2026-01-13")
                        .param("to", "2026-01-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void listSchedule_withoutAuth_returns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/schedule")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "2026-01-13")
                        .param("to", "2026-01-19"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(containsString("/schedule")));
    }

    @Test
    void listSchedule_withInvalidToken_returns401WithJsonBody() throws Exception {
        when(jwtService.parseToken(anyString())).thenThrow(new IllegalArgumentException("Invalid token"));

        mockMvc.perform(get("/api/v1/jobs/schedule")
                        .header("Authorization", "Bearer invalid-token")
                        .header("X-Tenant-Id", tenantId.toString())
                        .param("from", "2026-01-13")
                        .param("to", "2026-01-19"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @TestConfiguration
    static class TestCorsConfig {
        @Bean
        @Primary
        CorsProperties corsProperties() {
            CorsProperties p = new CorsProperties();
            p.setAllowedOrigins(List.of("http://localhost:3000"));
            p.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            p.setAllowedHeaders(List.of("*"));
            p.setAllowCredentials(true);
            p.setMaxAge(3600L);
            return p;
        }
    }
}
