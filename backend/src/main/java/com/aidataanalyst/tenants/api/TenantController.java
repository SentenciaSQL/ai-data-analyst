package com.aidataanalyst.tenants.api;

import com.aidataanalyst.tenants.application.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant")
public class TenantController {

    private final TenantService tenants;

    public TenantController(TenantService tenants) {
        this.tenants = tenants;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tenant:read')")
    public TenantResponse current() {
        return tenants.current();
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('tenant:write')")
    public TenantResponse update(@Valid @RequestBody UpdateTenantRequest request) {
        return tenants.update(request);
    }
}
