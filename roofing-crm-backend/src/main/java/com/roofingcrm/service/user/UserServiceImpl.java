package com.roofingcrm.service.user;

import com.roofingcrm.api.v1.user.UserPickerDto;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final TenantAccessService tenantAccessService;
    private final TenantUserMembershipRepository membershipRepository;

    @Autowired
    public UserServiceImpl(TenantAccessService tenantAccessService,
                           TenantUserMembershipRepository membershipRepository) {
        this.tenantAccessService = tenantAccessService;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPickerDto> searchUsers(@NonNull UUID tenantId, @NonNull UUID userId, String q, int limit) {
        var tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();
        int capped = Math.min(Math.max(limit, 1), 50);

        List<TenantUserMembership> members = membershipRepository.searchUsersInTenant(
                tenant, qNorm, PageRequest.of(0, capped));

        return members.stream()
                .map(TenantUserMembership::getUser)
                .filter(u -> u != null)
                .map(this::toPickerDto)
                .collect(Collectors.toList());
    }

    private UserPickerDto toPickerDto(User u) {
        return new UserPickerDto(
                u.getId(),
                u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail(),
                u.getEmail()
        );
    }
}
