package com.roofingcrm.api.v1.auth;

import com.roofingcrm.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RegisterWithInviteRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @StrongPassword
    private String password;

    @NotBlank
    private String fullName;

    @NotNull
    private UUID token;
}
