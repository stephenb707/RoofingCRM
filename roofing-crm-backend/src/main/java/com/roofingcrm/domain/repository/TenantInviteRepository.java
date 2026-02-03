package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantInviteRepository extends JpaRepository<TenantInvite, UUID> {

    Optional<TenantInvite> findByToken(UUID token);

    List<TenantInvite> findByTenantAndAcceptedAtIsNull(Tenant tenant);

    Optional<TenantInvite> findByTenantAndEmailIgnoreCaseAndAcceptedAtIsNull(Tenant tenant, String email);
}
