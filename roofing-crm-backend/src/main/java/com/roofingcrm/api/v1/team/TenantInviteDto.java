package com.roofingcrm.api.v1.team;

import com.roofingcrm.domain.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class TenantInviteDto {

    private UUID inviteId;
    private String email;
    private UserRole role;
    private UUID token;
    private Instant expiresAt;
    private Instant acceptedAt;
    private Instant createdAt;
    private String createdByName;
}
