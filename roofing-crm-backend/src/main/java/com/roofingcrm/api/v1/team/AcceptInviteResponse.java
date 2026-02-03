package com.roofingcrm.api.v1.team;

import com.roofingcrm.domain.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AcceptInviteResponse {

    private UUID tenantId;
    private String tenantName;
    private String tenantSlug;
    private UserRole role;
}
