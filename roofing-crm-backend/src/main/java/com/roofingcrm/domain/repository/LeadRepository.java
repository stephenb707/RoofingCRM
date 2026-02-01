package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndStatusAndArchivedFalse(Tenant tenant, LeadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndCustomerIdAndArchivedFalse(Tenant tenant, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndStatusAndCustomerIdAndArchivedFalse(Tenant tenant, LeadStatus status, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndSourceAndArchivedFalse(Tenant tenant, LeadSource source, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndStatusAndSourceAndArchivedFalse(Tenant tenant, LeadStatus status, LeadSource source, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Lead> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
        select l from Lead l
        left join l.customer c
        where l.tenant = :tenant
          and l.archived = false
          and (:q is null or :q = '' or
               lower(coalesce(c.firstName, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.lastName, '')) like lower(concat('%', :q, '%'))
               or lower(concat(coalesce(c.firstName, ''), ' ', coalesce(c.lastName, ''))) like lower(concat('%', :q, '%'))
               or lower(concat(coalesce(c.lastName, ''), ' ', coalesce(c.firstName, ''))) like lower(concat('%', :q, '%')))
        order by l.createdAt desc
        """)
    List<Lead> searchForPicker(@Param("tenant") Tenant tenant, @Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
