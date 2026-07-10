package com.roofingcrm.api.v1.settings.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.IntegrationConnectionStatus;
import com.roofingcrm.domain.enums.IntegrationProvider;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.settings.TenantIntegrationSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantIntegrationSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class TenantIntegrationSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantIntegrationSettingsService integrationSettingsService;

    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        AuthenticatedUser authUser = new AuthenticatedUser(Objects.requireNonNull(userId), "owner@example.com");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(authUser, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void list_returnsProviders() throws Exception {
        IntegrationSettingsDto dto = new IntegrationSettingsDto();
        dto.setProvider(IntegrationProvider.TWILIO);
        dto.setHumanLabel("SMS / Twilio");
        dto.setCategory("Messaging");
        dto.setEnabled(false);
        dto.setStatus(IntegrationConnectionStatus.NOT_CONFIGURED);
        dto.setHasCredentials(false);
        dto.setConfig(Map.of());
        when(integrationSettingsService.listSettings(Objects.requireNonNull(tenantId), Objects.requireNonNull(userId))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/settings/integrations")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Objects.requireNonNull(hasSize(1))))
                .andExpect(jsonPath("$[0].provider").value("TWILIO"))
                .andExpect(jsonPath("$[0].hasCredentials").value(false));
    }

    @Test
    void put_updatesAndOmitsSecretsInResponse() throws Exception {
        IntegrationSettingsDto dto = new IntegrationSettingsDto();
        dto.setProvider(IntegrationProvider.QUICKBOOKS);
        dto.setHumanLabel("QuickBooks");
        dto.setCategory("Accounting");
        dto.setEnabled(true);
        dto.setDisplayName("QuickBooks Online");
        dto.setHasCredentials(true);
        dto.setStatus(IntegrationConnectionStatus.NOT_CONFIGURED);
        dto.setUpdatedAt(Instant.parse("2026-05-01T00:00:00Z"));
        dto.setConfig(Map.of("realmId", "123"));

        UpdateIntegrationSettingsRequest req = new UpdateIntegrationSettingsRequest();
        req.setEnabled(true);
        req.setSecrets(Map.of("clientSecret", "s3cr3t"));

        when(integrationSettingsService.updateSettings(eq(tenantId), eq(userId),
                eq(IntegrationProvider.QUICKBOOKS), any(UpdateIntegrationSettingsRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(put("/api/v1/settings/integrations/QUICKBOOKS")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("QUICKBOOKS"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.status").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.displayName").value("QuickBooks Online"))
                .andExpect(jsonPath("$.hasCredentials").value(true))
                .andExpect(jsonPath("$.config.realmId").value("123"))
                .andExpect(jsonPath("$.secrets").doesNotExist())
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andExpect(content().string(not(containsString("s3cr3t"))));

        ArgumentCaptor<UpdateIntegrationSettingsRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateIntegrationSettingsRequest.class);
        verify(integrationSettingsService).updateSettings(eq(tenantId), eq(userId),
                eq(IntegrationProvider.QUICKBOOKS), requestCaptor.capture());
        assertEquals("s3cr3t", requestCaptor.getValue().getSecrets().get("clientSecret"));
    }

    @Test
    void disable_callsService() throws Exception {
        IntegrationSettingsDto dto = new IntegrationSettingsDto();
        dto.setProvider(IntegrationProvider.HOVER);
        dto.setEnabled(false);
        dto.setStatus(IntegrationConnectionStatus.DISABLED);
        when(integrationSettingsService.disableIntegration(Objects.requireNonNull(tenantId), Objects.requireNonNull(userId), Objects.requireNonNull(IntegrationProvider.HOVER)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/settings/integrations/HOVER/disable")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }
}
