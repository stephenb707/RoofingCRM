package com.roofingcrm.service.auth;

import com.roofingcrm.api.v1.auth.AuthResponse;
import com.roofingcrm.api.v1.auth.LoginRequest;
import com.roofingcrm.api.v1.auth.RegisterRequest;
import com.roofingcrm.api.v1.auth.TenantSummaryDto;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserMembershipRepository membershipRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserRepository userRepository,
                           TenantRepository tenantRepository,
                           TenantUserMembershipRepository membershipRepository,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse registerOwner(RegisterRequest request) {
        // Check if user already exists
        userRepository.findByEmailIgnoreCase(request.getEmail())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("User with that email already exists");
                });

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user = userRepository.save(user);

        Tenant tenant = new Tenant();
        tenant.setName(request.getTenantName());
        // naive slug; in a real system ensure uniqueness and nicer slugging
        tenant.setSlug(request.getTenantName().toLowerCase().replace(" ", "-"));
        tenant = tenantRepository.save(tenant);

        TenantUserMembership membership = new TenantUserMembership();
        membership.setTenant(tenant);
        membership.setUser(user);
        membership.setRole(UserRole.OWNER);
        membershipRepository.save(membership);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        List<TenantUserMembership> memberships = membershipRepository.findByUser(user);

        List<TenantSummaryDto> tenantDtos = memberships.stream()
                .map(m -> {
                    TenantSummaryDto dto = new TenantSummaryDto();
                    dto.setTenantId(m.getTenant().getId());
                    dto.setTenantName(m.getTenant().getName());
                    dto.setTenantSlug(m.getTenant().getSlug());
                    dto.setRole(m.getRole());
                    return dto;
                })
                .toList();

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setTenants(tenantDtos);
        return response;
    }
}
