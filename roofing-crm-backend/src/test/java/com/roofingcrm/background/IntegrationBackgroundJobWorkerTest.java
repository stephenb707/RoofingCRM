package com.roofingcrm.background;

import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import com.roofingcrm.domain.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class IntegrationBackgroundJobWorkerTest {

    @Mock
    private IntegrationBackgroundJobService jobService;

    @Mock
    private BackgroundJobProperties properties;

    private IntegrationBackgroundJobWorker worker;

    @BeforeEach
    void setUp() {
        worker = new IntegrationBackgroundJobWorker(
                jobService, List.of(new NoOpBackgroundJobHandler()), properties);
    }

    @Test
    void poll_noopsWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);
        worker.poll();
        verifyNoInteractions(jobService);
    }

    @Test
    void poll_marksUnsupportedJobDead() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(10);
        IntegrationBackgroundJob job = claimedJob("NOT_SUPPORTED_HERE");
        when(jobService.claimDueJobs(any(Instant.class), anyInt(), anyString())).thenReturn(List.of(job));

        worker.poll();

        verify(jobService).markDeadUnsupported(
                eq(job.getId()), eq("Unsupported job type: NOT_SUPPORTED_HERE"));
        verify(jobService, never()).markSucceeded(any(UUID.class));
    }

    @Test
    void poll_runsNoOpHandler() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(10);
        IntegrationBackgroundJob job = claimedJob(BackgroundJobTypes.NO_OP);
        when(jobService.claimDueJobs(any(Instant.class), anyInt(), anyString())).thenReturn(List.of(job));

        worker.poll();

        verify(jobService).markSucceeded(job.getId());
    }

    private static IntegrationBackgroundJob claimedJob(String jobType) {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Test Roofing");
        tenant.setSlug("test-roofing");

        IntegrationBackgroundJob job = new IntegrationBackgroundJob();
        job.setId(UUID.randomUUID());
        job.setTenant(tenant);
        job.setJobType(jobType);
        job.setStatus(BackgroundJobStatus.RUNNING);
        job.setPayloadJson(Map.of());
        job.setAttempts(0);
        job.setMaxAttempts(5);
        job.setNextRunAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return job;
    }
}
