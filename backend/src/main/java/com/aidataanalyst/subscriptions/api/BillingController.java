package com.aidataanalyst.subscriptions.api;

import com.aidataanalyst.shared.exception.ApiException;
import com.aidataanalyst.shared.tenant.TenantContext;
import com.aidataanalyst.tenants.infrastructure.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final TenantRepository tenants;

    public BillingController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('billing:read')")
    public BillingResponse current() {
        var tenant = tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Workspace not found."));
        return new BillingResponse(
                tenant.getPlanCode(),
                tenant.getStatus().name(),
                "This workspace is on the trial plan. Subscription changes are not available in this release.");
    }
}
