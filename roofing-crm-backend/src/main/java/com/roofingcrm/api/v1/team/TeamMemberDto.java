package com.roofingcrm.api.v1.team;

import com.roofingcrm.domain.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TeamMemberDto {

    private UUID userId;
    private String email;
    private String fullName;
    private UserRole role;
}
