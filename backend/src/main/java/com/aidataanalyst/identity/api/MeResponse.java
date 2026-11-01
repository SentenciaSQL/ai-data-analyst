package com.aidataanalyst.identity.api;

import com.aidataanalyst.tenants.api.TenantResponse;
import com.aidataanalyst.users.api.UserResponse;

public record MeResponse(UserResponse user, TenantResponse tenant) {
}
