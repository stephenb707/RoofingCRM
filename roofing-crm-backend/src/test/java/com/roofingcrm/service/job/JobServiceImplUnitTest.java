package com.roofingcrm.service.job;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.UpdateJobRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class JobServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ActivityEventService activityEventService;

    private JobServiceImpl service;

    private Tenant tenant;
    private Job job;
    private Customer customer;
    private UUID tenantId;
    private UUID userId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        service = new JobServiceImpl(
                tenantAccessService, jobRepository, customerRepository, leadRepository, activityEventService);

        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenantId = tenant.getId();

        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setTenant(tenant);

        job = new Job();
        job.setId(jobId);
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setStatus(JobStatus.SCHEDULED);
        job.setJobType(JobType.REPLACEMENT);
        job.setScheduledStartDate(LocalDate.of(2026, 1, 15));
        job.setScheduledEndDate(LocalDate.of(2026, 1, 17));
        job.setAssignedCrew("Alpha");
    }

    @Test
    void updateJob_whenScheduleChanges_emitsJOB_SCHEDULE_CHANGED() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Job savedJob = new Job();
        savedJob.setId(jobId);
        savedJob.setTenant(tenant);
        savedJob.setCustomer(customer);
        savedJob.setStatus(JobStatus.SCHEDULED);
        savedJob.setJobType(JobType.REPLACEMENT);
        savedJob.setScheduledStartDate(LocalDate.of(2026, 1, 20));
        savedJob.setScheduledEndDate(LocalDate.of(2026, 1, 22));
        savedJob.setAssignedCrew("Bravo");
        savedJob.setUpdatedByUserId(userId);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        UpdateJobRequest request = new UpdateJobRequest();
        request.setScheduledStartDate(LocalDate.of(2026, 1, 20));
        request.setScheduledEndDate(LocalDate.of(2026, 1, 22));
        request.setCrewName("Bravo");

        service.updateJob(tenantId, userId, jobId, request);

        ArgumentCaptor<ActivityEventType> typeCaptor = ArgumentCaptor.forClass(ActivityEventType.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(activityEventService).recordEvent(
                eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                typeCaptor.capture(), messageCaptor.capture(), any());

        assertEquals(ActivityEventType.JOB_SCHEDULE_CHANGED, typeCaptor.getValue());
        assertTrue(messageCaptor.getValue().contains("Schedule") || messageCaptor.getValue().contains("Crew"));
    }

    @Test
    void updateJob_whenScheduleUnchanged_doesNotEmitEvent() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Job savedJob = new Job();
        savedJob.setId(jobId);
        savedJob.setTenant(tenant);
        savedJob.setCustomer(customer);
        savedJob.setStatus(JobStatus.SCHEDULED);
        savedJob.setJobType(JobType.REPLACEMENT);
        savedJob.setScheduledStartDate(job.getScheduledStartDate());
        savedJob.setScheduledEndDate(job.getScheduledEndDate());
        savedJob.setAssignedCrew(job.getAssignedCrew());
        savedJob.setUpdatedByUserId(userId);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        UpdateJobRequest request = new UpdateJobRequest();
        request.setInternalNotes("Updated notes");

        service.updateJob(tenantId, userId, jobId, request);

        verify(activityEventService, never()).recordEvent(
                any(), any(), any(), any(), eq(ActivityEventType.JOB_SCHEDULE_CHANGED), any(), any());
    }

    @Test
    void createJob_whenScheduledStartDateSetAndEndDateNull_defaultsEndDateToStartDate() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(customerRepository.findByIdAndTenantAndArchivedFalse(customer.getId(), tenant))
                .thenReturn(Optional.of(customer));

        AddressDto address = new AddressDto();
        address.setLine1("123 Main St");
        address.setCity("Chicago");
        address.setState("IL");
        address.setZip("60601");

        CreateJobRequest request = new CreateJobRequest();
        request.setCustomerId(customer.getId());
        request.setType(JobType.REPLACEMENT);
        request.setPropertyAddress(address);
        request.setScheduledStartDate(LocalDate.of(2026, 2, 10));
        request.setScheduledEndDate(null);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        when(jobRepository.save(jobCaptor.capture())).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(jobId);
            return j;
        });

        service.createJob(tenantId, userId, request);

        Job saved = jobCaptor.getValue();
        assertEquals(LocalDate.of(2026, 2, 10), saved.getScheduledStartDate());
        assertEquals(LocalDate.of(2026, 2, 10), saved.getScheduledEndDate());
        assertEquals(JobStatus.SCHEDULED, saved.getStatus());
    }

    @Test
    void updateJob_unscheduledToScheduled_flipsStatusToScheduled() {
        job.setStatus(JobStatus.UNSCHEDULED);
        job.setScheduledStartDate(null);
        job.setScheduledEndDate(null);

        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Job savedJob = new Job();
        savedJob.setId(jobId);
        savedJob.setTenant(tenant);
        savedJob.setCustomer(customer);
        savedJob.setStatus(JobStatus.SCHEDULED);
        savedJob.setScheduledStartDate(LocalDate.of(2026, 2, 15));
        savedJob.setScheduledEndDate(LocalDate.of(2026, 2, 15));
        savedJob.setUpdatedByUserId(userId);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        UpdateJobRequest request = new UpdateJobRequest();
        request.setScheduledStartDate(LocalDate.of(2026, 2, 15));
        request.setScheduledEndDate(LocalDate.of(2026, 2, 15));

        service.updateJob(tenantId, userId, jobId, request);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();
        assertEquals(JobStatus.SCHEDULED, saved.getStatus());
        assertEquals(LocalDate.of(2026, 2, 15), saved.getScheduledStartDate());
    }

    @Test
    void updateJob_scheduledToUnscheduled_flipsStatusToUnscheduled() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Job savedJob = new Job();
        savedJob.setId(jobId);
        savedJob.setTenant(tenant);
        savedJob.setCustomer(customer);
        savedJob.setStatus(JobStatus.UNSCHEDULED);
        savedJob.setScheduledStartDate(null);
        savedJob.setScheduledEndDate(null);
        savedJob.setUpdatedByUserId(userId);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        UpdateJobRequest request = new UpdateJobRequest();
        request.setClearSchedule(true);

        service.updateJob(tenantId, userId, jobId, request);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();
        assertEquals(JobStatus.UNSCHEDULED, saved.getStatus());
        assertNull(saved.getScheduledStartDate());
        assertNull(saved.getScheduledEndDate());
    }

    @Test
    void updateJob_scheduledStaysScheduled_idempotent() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Job savedJob = new Job();
        savedJob.setId(jobId);
        savedJob.setTenant(tenant);
        savedJob.setCustomer(customer);
        savedJob.setStatus(JobStatus.SCHEDULED);
        savedJob.setScheduledStartDate(LocalDate.of(2026, 2, 20));
        savedJob.setScheduledEndDate(LocalDate.of(2026, 2, 22));
        savedJob.setUpdatedByUserId(userId);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        UpdateJobRequest request = new UpdateJobRequest();
        request.setScheduledStartDate(LocalDate.of(2026, 2, 20));
        request.setScheduledEndDate(LocalDate.of(2026, 2, 22));

        service.updateJob(tenantId, userId, jobId, request);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();
        assertEquals(JobStatus.SCHEDULED, saved.getStatus());
    }

    @Test
    void updateJob_unscheduledStaysUnscheduled_idempotent() {
        job.setStatus(JobStatus.UNSCHEDULED);
        job.setScheduledStartDate(null);
        job.setScheduledEndDate(null);

        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Job savedJob = new Job();
        savedJob.setId(jobId);
        savedJob.setTenant(tenant);
        savedJob.setCustomer(customer);
        savedJob.setStatus(JobStatus.UNSCHEDULED);
        savedJob.setScheduledStartDate(null);
        savedJob.setScheduledEndDate(null);
        savedJob.setUpdatedByUserId(userId);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        UpdateJobRequest request = new UpdateJobRequest();
        request.setInternalNotes("Just notes, no schedule");

        service.updateJob(tenantId, userId, jobId, request);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();
        assertEquals(JobStatus.UNSCHEDULED, saved.getStatus());
    }
}
