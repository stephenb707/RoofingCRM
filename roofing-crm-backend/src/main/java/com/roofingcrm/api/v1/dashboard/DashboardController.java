package com.roofingcrm.api.v1.dashboard;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary(@RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return dashboardService.getSummary(tenantId, userId);
    }
}
