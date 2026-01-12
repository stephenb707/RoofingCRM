package com.roofingcrm.service.auth;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.api.v1.auth.AuthResponse;
import com.roofingcrm.api.v1.auth.LoginRequest;
import com.roofingcrm.api.v1.auth.RegisterRequest;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantUserMembershipRepository membershipRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void registerOwner_createsUserTenantAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("owner@example.com");
        request.setPassword("password123");
        request.setFullName("John Owner");
        request.setTenantName("Test Roofing Company");

        AuthResponse response = authService.registerOwner(request);

        // Assert response
        assertNotNull(response.getToken());
        assertNotNull(response.getUserId());
        assertEquals("owner@example.com", response.getEmail());
        assertEquals("John Owner", response.getFullName());
        assertNotNull(response.getTenants());
        assertEquals(1, response.getTenants().size());
        assertEquals(UserRole.OWNER, response.getTenants().get(0).getRole());
        assertEquals("Test Roofing Company", response.getTenants().get(0).getTenantName());

        // Verify in database
        User savedUser = userRepository.findByEmailIgnoreCase("owner@example.com").orElseThrow();
        assertEquals("owner@example.com", savedUser.getEmail());
        assertTrue(savedUser.isEnabled());

        List<TenantUserMembership> memberships = membershipRepository.findByUser(savedUser);
        assertEquals(1, memberships.size());
        assertEquals(UserRole.OWNER, memberships.get(0).getRole());
    }

    @Test
    void registerOwner_duplicateEmail_throwsException() {
        RegisterRequest request1 = new RegisterRequest();
        request1.setEmail("duplicate@example.com");
        request1.setPassword("password123");
        request1.setFullName("First User");
        request1.setTenantName("First Company");
        authService.registerOwner(request1);

        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("duplicate@example.com");
        request2.setPassword("password456");
        request2.setFullName("Second User");
        request2.setTenantName("Second Company");

        assertThrows(IllegalArgumentException.class, () -> authService.registerOwner(request2));
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("securePassword");
        registerRequest.setFullName("Login User");
        registerRequest.setTenantName("Login Company");
        AuthResponse registerResponse = authService.registerOwner(registerRequest);

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("securePassword");

        AuthResponse loginResponse = authService.login(loginRequest);

        assertNotNull(loginResponse.getToken());
        assertEquals(registerResponse.getUserId(), loginResponse.getUserId());
        assertEquals("login@example.com", loginResponse.getEmail());
        assertEquals(1, loginResponse.getTenants().size());
    }

    @Test
    void login_invalidPassword_throwsException() {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("wrongpass@example.com");
        registerRequest.setPassword("correctPassword");
        registerRequest.setFullName("Test User");
        registerRequest.setTenantName("Test Company");
        authService.registerOwner(registerRequest);

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@example.com");
        loginRequest.setPassword("wrongPassword");

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_nonExistentUser_throwsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("anyPassword");

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }
}
