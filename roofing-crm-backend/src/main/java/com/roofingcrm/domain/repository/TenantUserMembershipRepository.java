package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserMembershipRepository extends JpaRepository<TenantUserMembership, UUID> {

    List<TenantUserMembership> findByUser(User user);

    Optional<TenantUserMembership> findByTenantAndUser(Tenant tenant, User user);

    List<TenantUserMembership> findByTenantAndRole(Tenant tenant, UserRole role);

    @Query("""
        select m from TenantUserMembership m
        join fetch m.user u
        where m.tenant = :tenant
          and m.archived = false
          and u.archived = false
          and u.enabled = true
          and (:q is null or :q = '' or
               lower(u.fullName) like lower(concat('%', :q, '%'))
               or lower(u.email) like lower(concat('%', :q, '%')))
        order by u.fullName, u.email
        """)
    List<TenantUserMembership> searchUsersInTenant(@Param("tenant") Tenant tenant, @Param("q") String q, Pageable pageable);
}
