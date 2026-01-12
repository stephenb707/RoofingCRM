package com.roofingcrm.service.job;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.job.UpdateJobRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final TenantRepository tenantRepository;
    private final JobRepository jobRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;

    @Autowired
    public JobServiceImpl(TenantRepository tenantRepository,
                          JobRepository jobRepository,
                          CustomerRepository customerRepository,
                          LeadRepository leadRepository) {
        this.tenantRepository = tenantRepository;
        this.jobRepository = jobRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
    }

    @Override
    public JobDto createJob(@NonNull UUID tenantId, UUID userId, CreateJobRequest request) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Customer customer;
        Lead lead = null;

        if (request.getLeadId() != null) {
            // Create job from a lead
            lead = leadRepository.findByIdAndTenantAndArchivedFalse(request.getLeadId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
            customer = lead.getCustomer();
            if (customer == null) {
                throw new IllegalArgumentException("Lead has no associated customer");
            }
        } else if (request.getCustomerId() != null) {
            // Create job directly from customer
            customer = customerRepository.findByIdAndTenantAndArchivedFalse(request.getCustomerId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        } else {
            throw new IllegalArgumentException("Either leadId or customerId must be provided");
        }

        Job job = new Job();
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setLead(lead);
        job.setCreatedByUserId(userId);
        job.setUpdatedByUserId(userId);

        job.setJobType(request.getType());
        job.setStatus(JobStatus.SCHEDULED);

        if (request.getPropertyAddress() != null) {
            Address address = new Address();
            applyAddress(address, request.getPropertyAddress());
            job.setPropertyAddress(address);
        }

        job.setScheduledStartDate(request.getScheduledStartDate());
        job.setScheduledEndDate(request.getScheduledEndDate());
        job.setJobNotes(request.getInternalNotes());
        job.setAssignedCrew(request.getCrewName());

        Job saved = jobRepository.save(job);
        return toDto(saved);
    }

    @Override
    public JobDto updateJob(@NonNull UUID tenantId, UUID userId, UUID jobId, UpdateJobRequest request) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        job.setUpdatedByUserId(userId);

        if (request.getType() != null) {
            job.setJobType(request.getType());
        }

        if (request.getPropertyAddress() != null) {
            Address address = job.getPropertyAddress();
            if (address == null) {
                address = new Address();
            }
            applyAddress(address, request.getPropertyAddress());
            job.setPropertyAddress(address);
        }

        if (request.getScheduledStartDate() != null) {
            job.setScheduledStartDate(request.getScheduledStartDate());
        }

        if (request.getScheduledEndDate() != null) {
            job.setScheduledEndDate(request.getScheduledEndDate());
        }

        if (request.getInternalNotes() != null) {
            job.setJobNotes(request.getInternalNotes());
        }

        if (request.getCrewName() != null) {
            job.setAssignedCrew(request.getCrewName());
        }

        Job saved = jobRepository.save(job);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDto getJob(@NonNull UUID tenantId, UUID jobId) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return toDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> listJobs(@NonNull UUID tenantId, JobStatus statusFilter, UUID customerIdFilter, Pageable pageable) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Page<Job> page;
        if (statusFilter != null) {
            page = jobRepository.findByTenantAndStatusAndArchivedFalse(tenant, statusFilter, pageable);
        } else if (customerIdFilter != null) {
            page = jobRepository.findByTenantAndCustomerIdAndArchivedFalse(tenant, customerIdFilter, pageable);
        } else {
            page = jobRepository.findByTenantAndArchivedFalse(tenant, pageable);
        }

        return page.map(this::toDto);
    }

    @Override
    public JobDto updateJobStatus(@NonNull UUID tenantId, UUID userId, UUID jobId, JobStatus newStatus) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        job.setStatus(newStatus);
        job.setUpdatedByUserId(userId);

        Job saved = jobRepository.save(job);
        return toDto(saved);
    }

    private Tenant getTenantOrThrow(@NonNull UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private void applyAddress(Address entity, AddressDto dto) {
        entity.setLine1(dto.getLine1());
        entity.setLine2(dto.getLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZip(dto.getZip());
        entity.setCountryCode(dto.getCountryCode());
    }

    private JobDto toDto(Job entity) {
        JobDto dto = new JobDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setType(entity.getJobType());
        dto.setScheduledStartDate(entity.getScheduledStartDate());
        dto.setScheduledEndDate(entity.getScheduledEndDate());
        dto.setInternalNotes(entity.getJobNotes());
        dto.setCrewName(entity.getAssignedCrew());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
        }

        if (entity.getLead() != null) {
            dto.setLeadId(entity.getLead().getId());
        }

        if (entity.getPropertyAddress() != null) {
            AddressDto a = new AddressDto();
            a.setLine1(entity.getPropertyAddress().getLine1());
            a.setLine2(entity.getPropertyAddress().getLine2());
            a.setCity(entity.getPropertyAddress().getCity());
            a.setState(entity.getPropertyAddress().getState());
            a.setZip(entity.getPropertyAddress().getZip());
            a.setCountryCode(entity.getPropertyAddress().getCountryCode());
            dto.setPropertyAddress(a);
        }

        return dto;
    }
}
