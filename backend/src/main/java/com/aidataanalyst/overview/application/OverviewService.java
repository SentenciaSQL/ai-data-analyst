package com.aidataanalyst.overview.application;

import com.aidataanalyst.analyst.infrastructure.AiEngineClient;
import com.aidataanalyst.audit.api.AuditEventResponse;
import com.aidataanalyst.audit.infrastructure.AuditEventRepository;
import com.aidataanalyst.overview.api.OverviewResponse;
import com.aidataanalyst.shared.exception.ApiException;
import com.aidataanalyst.shared.tenant.TenantContext;
import com.aidataanalyst.tenants.api.TenantResponse;
import com.aidataanalyst.tenants.infrastructure.TenantRepository;
import com.aidataanalyst.users.domain.UserStatus;
import com.aidataanalyst.users.infrastructure.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OverviewService {

    private static final Logger log = LoggerFactory.getLogger(OverviewService.class);

    private final TenantRepository tenants;
    private final UserRepository users;
    private final AuditEventRepository audits;
    private final AiEngineClient aiEngine;

    public OverviewService(
            TenantRepository tenants,
            UserRepository users,
            AuditEventRepository audits,
            AiEngineClient aiEngine) {
        this.tenants = tenants;
        this.users = users;
        this.audits = audits;
        this.aiEngine = aiEngine;
    }

    public OverviewResponse current() {
        var tenant = tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Workspace not found."));
        String engineStatus = "unavailable";
        String provider = null;
        boolean providerConfigured = false;
        try {
            var health = aiEngine.health();
            engineStatus = health.status() == null ? "unavailable" : health.status();
            provider = health.provider();
            providerConfigured = health.providerConfigured();
        } catch (RuntimeException ex) {
            log.warn("AI engine health check failed: {}", ex.toString());
        }
        List<AuditEventResponse> activity = canReadAudit()
                ? audits.findTop8ByTenantIdOrderByCreatedAtDesc(tenant.getId()).stream().map(AuditEventResponse::from).toList()
                : List.of();
        return new OverviewResponse(
                TenantResponse.from(tenant),
                users.countByTenantId(tenant.getId()),
                users.countByTenantIdAndStatus(tenant.getId(), UserStatus.ACTIVE),
                engineStatus,
                provider,
                providerConfigured,
                activity);
    }

    private boolean canReadAudit() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "audit:read".equals(authority.getAuthority()));
    }
}
