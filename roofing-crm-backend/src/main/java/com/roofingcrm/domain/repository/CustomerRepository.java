package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    Optional<Customer> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
