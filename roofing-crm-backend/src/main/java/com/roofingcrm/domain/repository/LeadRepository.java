package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadSource;
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

    long countByTenantAndArchivedFalse(Tenant tenant);

    long countByTenantAndStatusDefinitionAndArchivedFalse(Tenant tenant, PipelineStatusDefinition statusDefinition);

    @Query("""
            select count(l) from Lead l
            join l.statusDefinition d
            where l.tenant = :tenant and l.archived = false
              and d.systemKey not in ('WON', 'LOST')
            """)
    long countActivePipelineByTenant(@Param("tenant") Tenant tenant);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Lead> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Lead> findByTenantAndStatusDefinitionAndArchivedFalse(
            Tenant tenant, PipelineStatusDefinition statusDefinition, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    List<Lead> findByTenantAndStatusDefinitionAndArchivedFalseOrderByPipelinePositionAscCreatedAtAsc(
            Tenant tenant, PipelineStatusDefinition statusDefinition);

    @Query("""
            select coalesce(max(l.pipelinePosition), -1) from Lead l
            where l.tenant = :tenant and l.statusDefinition = :statusDefinition and l.archived = false
            """)
    int findMaxPipelinePositionByTenantAndStatusDefinitionAndArchivedFalse(
            @Param("tenant") Tenant tenant,
            @Param("statusDefinition") PipelineStatusDefinition statusDefinition);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Lead> findByTenantAndCustomerIdAndArchivedFalse(Tenant tenant, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Lead> findByTenantAndStatusDefinitionAndCustomerIdAndArchivedFalse(
            Tenant tenant, PipelineStatusDefinition statusDefinition, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Lead> findByTenantAndSourceAndArchivedFalse(Tenant tenant, LeadSource source, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Lead> findByTenantAndStatusDefinitionAndSourceAndArchivedFalse(
            Tenant tenant, PipelineStatusDefinition statusDefinition, LeadSource source, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Optional<Lead> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @Query("""
        select distinct l from Lead l
        left join fetch l.statusDefinition
        left join fetch l.customer c
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
