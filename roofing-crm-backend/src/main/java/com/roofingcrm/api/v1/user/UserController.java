package com.roofingcrm.api.v1.user;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public UserController(UserService userService, TenantAccessService tenantAccessService) {
        this.userService = userService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ResponseEntity<List<UserPickerDto>> searchUsers(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        List<UserPickerDto> users = userService.searchUsers(tenantId, userId, q, Math.min(Math.max(limit, 1), 50));
        return ResponseEntity.ok(users);
    }
}
