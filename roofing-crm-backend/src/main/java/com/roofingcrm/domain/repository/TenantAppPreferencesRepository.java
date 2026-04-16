package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantAppPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantAppPreferencesRepository extends JpaRepository<TenantAppPreferences, UUID> {

    Optional<TenantAppPreferences> findByTenant(Tenant tenant);
}
