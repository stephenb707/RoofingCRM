package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    Optional<Customer> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @Query("""
        select c from Customer c
        where c.tenant = :tenant
          and c.archived = false
          and (
               lower(c.firstName) like lower(concat('%', :q, '%'))
            or lower(c.lastName) like lower(concat('%', :q, '%'))
            or lower(c.email) like lower(concat('%', :q, '%'))
            or (
                 :digitsOnly IS NOT NULL AND :digitsOnly <> ''
                 AND replace(replace(replace(replace(c.primaryPhone, '-', ''), ' ', ''), '(', ''), ')', '') like concat('%', :digitsOnly, '%')
               )
          )
        """)
    Page<Customer> search(@Param("tenant") Tenant tenant, @Param("q") String q, @Param("digitsOnly") String digitsOnly, Pageable pageable);
}
