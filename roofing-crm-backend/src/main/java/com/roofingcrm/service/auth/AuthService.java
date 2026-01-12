package com.roofingcrm.service.auth;

import com.roofingcrm.api.v1.auth.AuthResponse;
import com.roofingcrm.api.v1.auth.LoginRequest;
import com.roofingcrm.api.v1.auth.RegisterRequest;

public interface AuthService {

    AuthResponse registerOwner(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
