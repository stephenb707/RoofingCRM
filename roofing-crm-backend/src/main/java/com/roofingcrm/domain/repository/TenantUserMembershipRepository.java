package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserMembershipRepository extends JpaRepository<TenantUserMembership, UUID> {

    List<TenantUserMembership> findByUser(User user);

    Optional<TenantUserMembership> findByTenantAndUser(Tenant tenant, User user);

    List<TenantUserMembership> findByTenantAndRole(Tenant tenant, UserRole role);
}
