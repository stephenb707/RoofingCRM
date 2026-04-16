package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPhotoReportRepository extends JpaRepository<CustomerPhotoReport, UUID> {

    @EntityGraph(attributePaths = {"customer", "job"})
    List<CustomerPhotoReport> findByTenantAndArchivedFalseOrderByUpdatedAtDesc(Tenant tenant);

    /**
     * Loads report + sections + customer + job in one round-trip.
     * Section photos are loaded separately (see CustomerPhotoReportSectionPhotoRepository) to avoid
     * Hibernate MultipleBagFetchException from fetching two List (bag) collections in one query.
     */
    @EntityGraph(attributePaths = {"customer", "job", "sections"})
    @Query("select r from CustomerPhotoReport r where r.id = :id and r.tenant = :tenant and r.archived = false")
    Optional<CustomerPhotoReport> loadDetailed(@Param("id") UUID id, @Param("tenant") Tenant tenant);
}
