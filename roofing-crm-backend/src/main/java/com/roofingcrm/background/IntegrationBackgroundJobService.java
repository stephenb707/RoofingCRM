package com.roofingcrm.background;

import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.IntegrationBackgroundJobRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class IntegrationBackgroundJobService {

    private final IntegrationBackgroundJobRepository jobRepository;
    private final TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public IntegrationBackgroundJobService(IntegrationBackgroundJobRepository jobRepository,
                                          TenantRepository tenantRepository) {
        this.jobRepository = jobRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public IntegrationBackgroundJob enqueue(@NonNull UUID tenantId,
                                           @NonNull String jobType,
                                           Map<String, Object> payload,
                                           int maxAttempts,
                                           Instant nextRunAt,
                                           String correlationId,
                                           String entityType,
                                           UUID entityId) {
        Tenant tenant = tenantRepository.findById(Objects.requireNonNull(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown tenant"));
        IntegrationBackgroundJob job = new IntegrationBackgroundJob();
        job.setTenant(tenant);
        job.setJobType(Objects.requireNonNull(jobType));
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setPayloadJson(payload);
        job.setAttempts(0);
        job.setMaxAttempts(maxAttempts > 0 ? maxAttempts : 5);
        job.setNextRunAt(nextRunAt != null ? nextRunAt : Instant.now());
        job.setCorrelationId(correlationId);
        job.setEntityType(entityType);
        job.setEntityId(entityId);
        return jobRepository.save(job);
    }

    /**
     * Claims due jobs for processing using {@code FOR UPDATE SKIP LOCKED}.
     */
    @Transactional
    public List<IntegrationBackgroundJob> claimDueJobs(@NonNull Instant now, int limit, @NonNull String workerId) {
        if (limit <= 0) {
            return List.of();
        }
        List<?> rawIds = entityManager.createNativeQuery("""
                        SELECT id FROM integration_background_jobs
                        WHERE status = 'PENDING' AND next_run_at <= :now
                        ORDER BY next_run_at ASC
                        LIMIT :lim
                        FOR UPDATE SKIP LOCKED
                        """)
                .setParameter("now", now)
                .setParameter("lim", limit)
                .getResultList();
        List<UUID> ids = new ArrayList<>(rawIds.size());
        for (Object row : rawIds) {
            UUID id = toUuid(row);
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        int updated = entityManager.createNativeQuery("""
                        UPDATE integration_background_jobs j
                        SET status = 'RUNNING',
                            locked_at = :now,
                            locked_by = :worker,
                            updated_at = :now
                        WHERE j.id IN (:ids) AND j.status = 'PENDING'
                        """)
                .setParameter("now", now)
                .setParameter("worker", workerId)
                .setParameter("ids", ids)
                .executeUpdate();
        if (updated == 0) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select j from IntegrationBackgroundJob j where j.id in :ids",
                        IntegrationBackgroundJob.class)
                .setParameter("ids", ids)
                .getResultList()
                .stream()
                .filter(j -> j.getStatus() == BackgroundJobStatus.RUNNING && workerId.equals(j.getLockedBy()))
                .toList();
    }

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    @Transactional
    public void markSucceeded(@NonNull UUID jobId) {
        IntegrationBackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));
        job.setStatus(BackgroundJobStatus.SUCCEEDED);
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setLastError(null);
        jobRepository.save(job);
    }

    @Transactional
    public void markFailedOrRetry(@NonNull UUID jobId, @NonNull String errorMessage, @NonNull Instant now) {
        IntegrationBackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));
        int attemptAfterFailure = job.getAttempts() + 1;
        job.setAttempts(attemptAfterFailure);
        job.setLastError(errorMessage != null && errorMessage.length() > 2000
                ? errorMessage.substring(0, 2000)
                : errorMessage);
        job.setLockedAt(null);
        job.setLockedBy(null);
        if (attemptAfterFailure >= job.getMaxAttempts()) {
            job.setStatus(BackgroundJobStatus.DEAD);
        } else {
            job.setStatus(BackgroundJobStatus.PENDING);
            job.setNextRunAt(now.plus(BackgroundJobBackoff.delayAfterFailureNumber(attemptAfterFailure)));
        }
        jobRepository.save(job);
    }

    @Transactional
    public void markDeadUnsupported(@NonNull UUID jobId, @NonNull String reason) {
        IntegrationBackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));
        job.setStatus(BackgroundJobStatus.DEAD);
        job.setLastError(reason != null && reason.length() > 2000 ? reason.substring(0, 2000) : reason);
        job.setLockedAt(null);
        job.setLockedBy(null);
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<IntegrationBackgroundJob> findPendingForTenant(@NonNull UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        return jobRepository.findByTenantAndStatus(tenant, BackgroundJobStatus.PENDING);
    }
}
