package com.roofingcrm.background;

import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class IntegrationBackgroundJobWorker {

    private static final Logger log = LoggerFactory.getLogger(IntegrationBackgroundJobWorker.class);
    private static final String WORKER_ID = "worker-" + UUID.randomUUID();

    private final IntegrationBackgroundJobService jobService;
    private final List<BackgroundJobHandler> handlers;
    private final BackgroundJobProperties properties;

    public IntegrationBackgroundJobWorker(IntegrationBackgroundJobService jobService,
                                         List<BackgroundJobHandler> handlers,
                                         BackgroundJobProperties properties) {
        this.jobService = jobService;
        this.handlers = handlers;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@backgroundJobProperties.getPollDelayMs()}")
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant now = Objects.requireNonNull(Instant.now());
        String workerId = Objects.requireNonNull(WORKER_ID);
        try {
            List<IntegrationBackgroundJob> batch = jobService.claimDueJobs(now, properties.getBatchSize(), workerId);
            for (IntegrationBackgroundJob job : batch) {
                processOne(job, now);
            }
        } catch (Exception ex) {
            log.warn("Background job poll failed: {}", ex.toString());
        }
    }

    private void processOne(IntegrationBackgroundJob job, Instant now) {
        UUID jobId = Objects.requireNonNull(job.getId(), "job id");
        Instant runAt = Objects.requireNonNull(now, "now");
        BackgroundJobHandler handler = handlers.stream()
                .filter(h -> h.supports(job.getJobType()))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            log.info("Unsupported background job type {}, marking DEAD id={}", job.getJobType(), jobId);
            String reason = "Unsupported job type: " + job.getJobType();
            jobService.markDeadUnsupported(jobId, reason);
            return;
        }
        try {
            handler.handle(job);
        } catch (Exception ex) {
            log.warn("Background job failed id={} type={}: {}", jobId, job.getJobType(), ex.toString());
            String err = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            jobService.markFailedOrRetry(jobId, Objects.requireNonNull(err, "error message"), runAt);
            return;
        }
        jobService.markSucceeded(jobId);
        log.info("Background job succeeded id={} type={}", jobId, job.getJobType());
    }
}
