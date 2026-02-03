package com.roofingcrm.api.v1.team;

import com.roofingcrm.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotNull
    private UserRole role;
}
