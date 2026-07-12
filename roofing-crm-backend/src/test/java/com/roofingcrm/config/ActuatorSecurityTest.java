package com.roofingcrm.config;

import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.security.RefreshTokenProperties;
import com.roofingcrm.security.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ActuatorSecurityTest.ActuatorTestController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SecurityErrorHandlers.class, ActuatorSecurityTest.TestCorsConfig.class,
        ActuatorSecurityTest.ActuatorTestController.class})
@SuppressWarnings("null")
class ActuatorSecurityTest {

    private static final String AUTHORIZATION = "Bearer test-jwt-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitProperties rateLimitProperties;

    @MockitoBean
    private RefreshTokenProperties refreshTokenProperties;

    @BeforeEach
    void setUp() {
        when(jwtService.parseToken("test-jwt-token"))
                .thenReturn(new AuthenticatedUser(UUID.randomUUID(), "user@example.com"));
    }

    @Test
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsIsDeniedToAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/actuator/metrics").header("Authorization", AUTHORIZATION))
                .andExpect(status().isForbidden());
    }

    @Test
    void prometheusIsDeniedToAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", AUTHORIZATION))
                .andExpect(status().isForbidden());
    }

    @RestController
    public static class ActuatorTestController {

        @GetMapping({"/actuator/health", "/actuator/metrics", "/actuator/prometheus"})
        String endpoint() {
            return "ok";
        }
    }

    @TestConfiguration
    static class TestCorsConfig {

        @Bean
        @Primary
        CorsProperties corsProperties() {
            CorsProperties properties = new CorsProperties();
            properties.setAllowedOrigins(List.of("http://localhost:3000"));
            properties.setAllowedMethods(List.of("GET"));
            properties.setAllowedHeaders(List.of("*"));
            properties.setAllowCredentials(true);
            properties.setMaxAge(3600L);
            return properties;
        }
    }
}
