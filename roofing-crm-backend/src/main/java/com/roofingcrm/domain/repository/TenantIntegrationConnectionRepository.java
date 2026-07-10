package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantIntegrationConnection;
import com.roofingcrm.domain.enums.IntegrationProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantIntegrationConnectionRepository extends JpaRepository<TenantIntegrationConnection, UUID> {

    List<TenantIntegrationConnection> findByTenant(Tenant tenant);

    Optional<TenantIntegrationConnection> findByTenantAndProvider(Tenant tenant, IntegrationProvider provider);
}
