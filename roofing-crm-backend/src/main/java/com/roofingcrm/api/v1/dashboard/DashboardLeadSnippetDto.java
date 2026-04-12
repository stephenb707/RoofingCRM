package com.roofingcrm.api.v1.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class DashboardLeadSnippetDto {

    private UUID id;

    private String statusKey;

    private String statusLabel;

    private String customerLabel;

    private String propertyLine1;

    private Instant updatedAt;
}
