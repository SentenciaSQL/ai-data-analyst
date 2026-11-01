package com.aidataanalyst.shared.tenant;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId, UUID userId) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
    }

    public static Optional<UUID> tenantId() {
        return Optional.ofNullable(TENANT_ID.get());
    }

    public static Optional<UUID> userId() {
        return Optional.ofNullable(USER_ID.get());
    }

    public static UUID requireTenantId() {
        return tenantId().orElseThrow(() -> new IllegalStateException("Tenant context is not set."));
    }

    public static UUID requireUserId() {
        return userId().orElseThrow(() -> new IllegalStateException("User context is not set."));
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
    }
}
