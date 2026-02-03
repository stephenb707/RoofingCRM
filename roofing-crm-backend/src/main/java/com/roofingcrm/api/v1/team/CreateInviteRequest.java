package com.roofingcrm.api.v1.team;

import com.roofingcrm.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInviteRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private UserRole role;
}
