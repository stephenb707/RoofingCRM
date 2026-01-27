package com.roofingcrm.service.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.NewLeadCustomerRequest;
import com.roofingcrm.api.v1.lead.UpdateLeadRequest;
import com.roofingcrm.api.v1.lead.ConvertLeadToJobRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.LeadConversionNotAllowedException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LeadServiceImpl implements LeadService {

    private final TenantAccessService tenantAccessService;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;

    @Autowired
    public LeadServiceImpl(TenantAccessService tenantAccessService,
                           LeadRepository leadRepository,
                           CustomerRepository customerRepository,
                           JobRepository jobRepository) {
        this.tenantAccessService = tenantAccessService;
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public LeadDto createLead(@NonNull UUID tenantId, @NonNull UUID userId, CreateLeadRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Customer customer = resolveCustomerForLead(tenant, userId, request);

        Lead lead = new Lead();
        lead.setTenant(tenant);
        lead.setCustomer(customer);
        lead.setCreatedByUserId(userId);
        lead.setUpdatedByUserId(userId);

        lead.setStatus(LeadStatus.NEW);
        lead.setSource(request.getSource() != null ? request.getSource() : LeadSource.OTHER);
        lead.setLeadNotes(request.getLeadNotes());
        lead.setPreferredContactMethod(request.getPreferredContactMethod());

        Address propertyAddress = new Address();
        applyAddress(propertyAddress, request.getPropertyAddress());
        lead.setPropertyAddress(propertyAddress);

        Lead saved = leadRepository.save(lead);
        return toDto(saved);
    }

    @Override
    public LeadDto updateLead(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, UpdateLeadRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        lead.setUpdatedByUserId(userId);

        if (request.getSource() != null) {
            lead.setSource(request.getSource());
        }

        if (request.getLeadNotes() != null) {
            lead.setLeadNotes(request.getLeadNotes());
        }

        if (request.getPreferredContactMethod() != null) {
            lead.setPreferredContactMethod(request.getPreferredContactMethod());
        }

        if (request.getPropertyAddress() != null) {
            if (lead.getPropertyAddress() == null) {
                lead.setPropertyAddress(new Address());
            }
            applyAddress(lead.getPropertyAddress(), request.getPropertyAddress());
        }

        Lead saved = leadRepository.save(lead);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadDto getLead(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        UUID convertedJobId = jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, leadId)
                .map(Job::getId)
                .orElse(null);
        return toDto(lead, convertedJobId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadDto> listLeads(@NonNull UUID tenantId, @NonNull UUID userId, LeadStatus statusFilter, UUID customerId, Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Page<Lead> page;
        if (statusFilter != null && customerId != null) {
            page = leadRepository.findByTenantAndStatusAndCustomerIdAndArchivedFalse(tenant, statusFilter, customerId, pageable);
        } else if (statusFilter != null) {
            page = leadRepository.findByTenantAndStatusAndArchivedFalse(tenant, statusFilter, pageable);
        } else if (customerId != null) {
            page = leadRepository.findByTenantAndCustomerIdAndArchivedFalse(tenant, customerId, pageable);
        } else {
            page = leadRepository.findByTenantAndArchivedFalse(tenant, pageable);
        }

        return page.map(l -> toDto(l, null));
    }

    @Override
    public LeadDto updateLeadStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, LeadStatus newStatus) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        lead.setStatus(newStatus);
        lead.setUpdatedByUserId(userId);

        Lead saved = leadRepository.save(lead);
        return toDto(saved);
    }

    @Override
    public JobDto convertLeadToJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, ConvertLeadToJobRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        if (lead.getStatus() == LeadStatus.LOST) {
            throw new LeadConversionNotAllowedException("Cannot convert a lost lead to a job");
        }

        // Check for existing job (idempotency)
        Optional<Job> existingJob = jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, leadId);
        if (existingJob.isPresent()) {
            return toJobDto(existingJob.get());
        }

        Customer customer = lead.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Lead has no associated customer");
        }

        // Create new job
        Job job = new Job();
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setLead(lead);
        job.setCreatedByUserId(userId);
        job.setUpdatedByUserId(userId);
        job.setStatus(JobStatus.SCHEDULED);
        job.setJobType(request.getType());
        job.setScheduledStartDate(request.getScheduledStartDate());
        job.setScheduledEndDate(request.getScheduledEndDate());
        job.setAssignedCrew(request.getCrewName());
        job.setJobNotes(request.getInternalNotes());

        // Copy property address from lead
        if (lead.getPropertyAddress() != null) {
            Address propertyAddress = new Address();
            copyAddress(lead.getPropertyAddress(), propertyAddress);
            job.setPropertyAddress(propertyAddress);
        }

        Job saved = jobRepository.save(job);

        // Update lead status to WON
        lead.setStatus(LeadStatus.WON);
        lead.setUpdatedByUserId(userId);
        leadRepository.save(lead);

        return toJobDto(saved);
    }

    private Customer resolveCustomerForLead(Tenant tenant, UUID userId, CreateLeadRequest request) {
        if (request.getCustomerId() != null) {
            return customerRepository.findByIdAndTenantAndArchivedFalse(request.getCustomerId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        NewLeadCustomerRequest newCustomer = request.getNewCustomer();
        if (newCustomer != null) {
            Customer customer = new Customer();
            customer.setTenant(tenant);
            customer.setCreatedByUserId(userId);
            customer.setUpdatedByUserId(userId);
            customer.setFirstName(newCustomer.getFirstName());
            customer.setLastName(newCustomer.getLastName());
            customer.setPrimaryPhone(newCustomer.getPrimaryPhone());
            customer.setEmail(newCustomer.getEmail());

            if (newCustomer.getBillingAddress() != null) {
                Address billing = new Address();
                applyAddress(billing, newCustomer.getBillingAddress());
                customer.setBillingAddress(billing);
            }

            return customerRepository.save(customer);
        }

        throw new IllegalArgumentException("Either customerId or newCustomer must be provided");
    }

    private void applyAddress(Address entity, AddressDto dto) {
        entity.setLine1(dto.getLine1());
        entity.setLine2(dto.getLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZip(dto.getZip());
        entity.setCountryCode(dto.getCountryCode());
    }

    private LeadDto toDto(Lead entity) {
        return toDto(entity, null);
    }

    private LeadDto toDto(Lead entity, UUID convertedJobId) {
        LeadDto dto = new LeadDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setSource(entity.getSource());
        dto.setLeadNotes(entity.getLeadNotes());
        dto.setPreferredContactMethod(entity.getPreferredContactMethod());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setConvertedJobId(convertedJobId);

        if (entity.getCustomer() != null) {
            Customer customer = entity.getCustomer();
            dto.setCustomerId(customer.getId());
            dto.setCustomerFirstName(customer.getFirstName());
            dto.setCustomerLastName(customer.getLastName());
            dto.setCustomerEmail(customer.getEmail());
            dto.setCustomerPhone(customer.getPrimaryPhone());
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

    private void copyAddress(Address source, Address target) {
        target.setLine1(source.getLine1());
        target.setLine2(source.getLine2());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setZip(source.getZip());
        target.setCountryCode(source.getCountryCode());
    }

    private JobDto toJobDto(Job entity) {
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
            Customer customer = entity.getCustomer();
            dto.setCustomerId(customer.getId());
            dto.setCustomerFirstName(customer.getFirstName());
            dto.setCustomerLastName(customer.getLastName());
            dto.setCustomerEmail(customer.getEmail());
            dto.setCustomerPhone(customer.getPrimaryPhone());
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
