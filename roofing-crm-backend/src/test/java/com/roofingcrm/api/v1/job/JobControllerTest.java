package com.roofingcrm.api.v1.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.job.JobService;
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

@WebMvcTest(controllers = JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null") // Hamcrest matchers have nullable return types
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

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
    void createJob_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        CreateJobRequest req = new CreateJobRequest();
        req.setCustomerId(customerId);
        req.setType(JobType.REPLACEMENT);

        AddressDto address = new AddressDto();
        address.setLine1("123 Main St");
        address.setCity("Chicago");
        address.setState("IL");
        address.setZip("60601");
        req.setPropertyAddress(address);

        JobDto responseDto = new JobDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setCustomerId(customerId);
        responseDto.setStatus(JobStatus.SCHEDULED);
        responseDto.setType(JobType.REPLACEMENT);
        responseDto.setCreatedAt(Instant.now());

        when(jobService.createJob(eq(tenantId), eq(userId), any(CreateJobRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.type", is("REPLACEMENT")));
    }

    @Test
    void getJob_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobDto dto = new JobDto();
        dto.setId(jobId);
        dto.setStatus(JobStatus.SCHEDULED);
        dto.setType(JobType.REPAIR);
        dto.setCreatedAt(Instant.now());

        when(jobService.getJob(tenantId, jobId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/jobs/{id}", jobId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(jobId.toString())))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    void createJob_withoutRequiredFields_returnsBadRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();

        // Missing type and propertyAddress
        CreateJobRequest req = new CreateJobRequest();
        req.setCustomerId(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateJobStatus_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        UpdateJobStatusRequest req = new UpdateJobStatusRequest();
        req.setStatus(JobStatus.IN_PROGRESS);

        JobDto responseDto = new JobDto();
        responseDto.setId(jobId);
        responseDto.setStatus(JobStatus.IN_PROGRESS);
        responseDto.setCreatedAt(Instant.now());

        when(jobService.updateJobStatus(eq(tenantId), eq(userId), eq(jobId), eq(JobStatus.IN_PROGRESS)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/jobs/{id}/status", jobId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }
}
