package com.roofingcrm.api.v1.dashboard;

import com.roofingcrm.domain.enums.LeadStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class DashboardLeadSnippetDto {

    private UUID id;

    private LeadStatus status;

    private String customerLabel;

    private String propertyLine1;

    private Instant updatedAt;
}
