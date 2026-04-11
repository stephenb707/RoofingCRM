package com.roofingcrm.service.dashboard;

import com.roofingcrm.api.v1.dashboard.DashboardSummaryDto;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface DashboardService {

    DashboardSummaryDto getSummary(@NonNull UUID tenantId, @NonNull UUID userId);
}
