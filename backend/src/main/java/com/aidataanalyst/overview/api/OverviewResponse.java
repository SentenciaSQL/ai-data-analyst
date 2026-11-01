package com.aidataanalyst.overview.api;

import com.aidataanalyst.audit.api.AuditEventResponse;
import com.aidataanalyst.tenants.api.TenantResponse;
import java.util.List;

public record OverviewResponse(
        TenantResponse tenant,
        long teamSize,
        long activeUsers,
        String aiEngineStatus,
        String aiProvider,
        boolean aiProviderConfigured,
        List<AuditEventResponse> recentActivity) {
}
