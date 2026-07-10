package com.roofingcrm.background;

import com.roofingcrm.RoofingCrmApplication;
import com.roofingcrm.TestDatabaseCleaner;
import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.IntegrationBackgroundJobRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Skipped automatically when Docker is unavailable (e.g. some CI agents).
 */
@SpringBootTest(classes = RoofingCrmApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class IntegrationBackgroundJobServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private IntegrationBackgroundJobService jobService;
    @Autowired
    private IntegrationBackgroundJobRepository jobRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantUserMembershipRepository membershipRepository;
    @Autowired
    private TestDatabaseCleaner dbCleaner;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        dbCleaner.reset();
        Tenant tenant = new Tenant();
        tenant.setName("Acme");
        tenant.setSlug("acme");
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setFullName("Admin");
        admin.setPasswordHash("x");
        admin.setEnabled(true);
        admin = userRepository.save(admin);

        TenantUserMembership m = new TenantUserMembership();
        m.setTenant(tenant);
        m.setUser(admin);
        m.setRole(UserRole.ADMIN);
        membershipRepository.save(m);
    }

    @Test
    void enqueue_claim_succeed_retry_dead() {
        IntegrationBackgroundJob enqueued = jobService.enqueue(
                Objects.requireNonNull(tenantId), BackgroundJobTypes.NO_OP, Map.of("hello", "world"), 3,
                Instant.now(), "corr", null, null);
        assertNotNull(enqueued.getId());
        assertEquals(BackgroundJobStatus.PENDING, enqueued.getStatus());

        List<IntegrationBackgroundJob> claimed = transactionTemplate.execute(status ->
                jobService.claimDueJobs(Objects.requireNonNull(Instant.now()), 5, "w1"));
        assertNotNull(claimed);
        assertEquals(1, claimed.size());
        UUID jobId = claimed.getFirst().getId();

        jobService.markSucceeded(Objects.requireNonNull(jobId));
        IntegrationBackgroundJob done = jobRepository.findById(jobId).orElseThrow();
        assertEquals(BackgroundJobStatus.SUCCEEDED, done.getStatus());

        IntegrationBackgroundJob flaky = jobService.enqueue(
                Objects.requireNonNull(tenantId), "WILL_FAIL", Map.of(), 2, Instant.now(), null, null, null);
        UUID flakyId = flaky.getId();

        List<IntegrationBackgroundJob> c2 = transactionTemplate.execute(status ->
                jobService.claimDueJobs(Objects.requireNonNull(Instant.now()), 5, "w2"));
        assertEquals(1, Objects.requireNonNull(c2).size());
        assertEquals(flakyId, c2.getFirst().getId());

        Instant now = Instant.now();
        jobService.markFailedOrRetry(Objects.requireNonNull(flakyId), "boom", Objects.requireNonNull(now));
        IntegrationBackgroundJob afterFail = jobRepository.findById(flakyId).orElseThrow();
        assertEquals(BackgroundJobStatus.PENDING, afterFail.getStatus());
        assertEquals(1, afterFail.getAttempts());
        assertTrue(afterFail.getNextRunAt().isAfter(now));

        transactionTemplate.execute(status -> {
            jobService.markFailedOrRetry(Objects.requireNonNull(flakyId), "boom2", Objects.requireNonNull(Instant.now()));
            return null;
        });
        IntegrationBackgroundJob dead = jobRepository.findById(flakyId).orElseThrow();
        assertEquals(BackgroundJobStatus.DEAD, dead.getStatus());

        IntegrationBackgroundJob badType = jobService.enqueue(
                Objects.requireNonNull(tenantId), "UNKNOWN_TYPE_X", Map.of(), 5, Instant.now(), null, null, null);
        jobService.markDeadUnsupported(Objects.requireNonNull(badType.getId()), "no handler");
        assertEquals(BackgroundJobStatus.DEAD, jobRepository.findById(Objects.requireNonNull(badType.getId())).orElseThrow().getStatus());
    }
}
