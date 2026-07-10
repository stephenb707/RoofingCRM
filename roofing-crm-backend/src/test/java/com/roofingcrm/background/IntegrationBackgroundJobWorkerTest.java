package com.roofingcrm.background;

import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
        IntegrationBackgroundJob job = new IntegrationBackgroundJob();
        job.setId(UUID.randomUUID());
        job.setJobType("NOT_SUPPORTED_HERE");
        when(jobService.claimDueJobs(Objects.requireNonNull(any(Instant.class)), anyInt(), Objects.requireNonNull(anyString()))).thenReturn(List.of(job));

        worker.poll();

        verify(jobService).markDeadUnsupported(Objects.requireNonNull(eq(job.getId())), Objects.requireNonNull(anyString()));
        verify(jobService, never()).markSucceeded(Objects.requireNonNull(any()));
    }

    @Test
    void poll_runsNoOpHandler() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(10);
        IntegrationBackgroundJob job = new IntegrationBackgroundJob();
        job.setId(UUID.randomUUID());
        job.setJobType(BackgroundJobTypes.NO_OP);
        when(jobService.claimDueJobs(Objects.requireNonNull(any(Instant.class)), anyInt(), Objects.requireNonNull(anyString()))).thenReturn(List.of(job));

        worker.poll();

        verify(jobService).markSucceeded(Objects.requireNonNull(job.getId()));
    }
}
