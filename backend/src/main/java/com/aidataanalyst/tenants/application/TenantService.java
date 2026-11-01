package com.aidataanalyst.tenants.application;

import com.aidataanalyst.audit.application.AuditService;
import com.aidataanalyst.shared.exception.ApiException;
import com.aidataanalyst.shared.tenant.TenantContext;
import com.aidataanalyst.tenants.api.TenantResponse;
import com.aidataanalyst.tenants.api.UpdateTenantRequest;
import com.aidataanalyst.tenants.infrastructure.TenantEntity;
import com.aidataanalyst.tenants.infrastructure.TenantRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenants;
    private final AuditService audit;

    public TenantService(TenantRepository tenants, AuditService audit) {
        this.tenants = tenants;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public TenantResponse current() {
        return TenantResponse.from(requireCurrent());
    }

    @Transactional
    public TenantResponse update(UpdateTenantRequest request) {
        TenantEntity tenant = requireCurrent();
        tenant.setName(request.name().trim());
        audit.record("TENANT_UPDATED", "tenant", tenant.getId().toString(), Map.of("name", tenant.getName()));
        return TenantResponse.from(tenant);
    }

    private TenantEntity requireCurrent() {
        UUID tenantId = TenantContext.requireTenantId();
        return tenants.findById(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Workspace not found."));
    }
}
