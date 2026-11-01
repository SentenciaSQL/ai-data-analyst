package com.aidataanalyst.tenants.api;

import com.aidataanalyst.tenants.domain.TenantStatus;
import com.aidataanalyst.tenants.infrastructure.TenantEntity;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        String planCode,
        Instant createdAt) {

    public static TenantResponse from(TenantEntity tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getPlanCode(),
                tenant.getCreatedAt());
    }
}
