package com.aidataanalyst.identity.api;

import com.aidataanalyst.tenants.api.TenantResponse;
import com.aidataanalyst.users.api.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user,
        TenantResponse tenant) {
}
