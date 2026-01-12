package com.roofingcrm.service.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.NewLeadCustomerRequest;
import com.roofingcrm.api.v1.lead.UpdateLeadRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class LeadServiceImpl implements LeadService {

    private final TenantRepository tenantRepository;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public LeadServiceImpl(TenantRepository tenantRepository,
                           LeadRepository leadRepository,
                           CustomerRepository customerRepository) {
        this.tenantRepository = tenantRepository;
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public LeadDto createLead(UUID tenantId, UUID userId, CreateLeadRequest request) {
        Tenant tenant = getTenantOrThrow(tenantId);

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
    public LeadDto updateLead(UUID tenantId, UUID userId, UUID leadId, UpdateLeadRequest request) {
        Tenant tenant = getTenantOrThrow(tenantId);

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
    public LeadDto getLead(UUID tenantId, UUID leadId) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        return toDto(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadDto> listLeads(UUID tenantId, LeadStatus statusFilter, Pageable pageable) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Page<Lead> page;
        if (statusFilter != null) {
            page = leadRepository.findByTenantAndStatusAndArchivedFalse(tenant, statusFilter, pageable);
        } else {
            page = leadRepository.findByTenantAndArchivedFalse(tenant, pageable);
        }

        return page.map(this::toDto);
    }

    @Override
    public LeadDto updateLeadStatus(UUID tenantId, UUID userId, UUID leadId, LeadStatus newStatus) {
        Tenant tenant = getTenantOrThrow(tenantId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        lead.setStatus(newStatus);
        lead.setUpdatedByUserId(userId);

        Lead saved = leadRepository.save(lead);
        return toDto(saved);
    }

    private Tenant getTenantOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
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
        LeadDto dto = new LeadDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setSource(entity.getSource());
        dto.setLeadNotes(entity.getLeadNotes());
        dto.setPreferredContactMethod(entity.getPreferredContactMethod());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
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
