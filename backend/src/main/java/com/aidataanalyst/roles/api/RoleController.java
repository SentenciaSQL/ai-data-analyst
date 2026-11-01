package com.aidataanalyst.roles.api;

import com.aidataanalyst.roles.infrastructure.RoleRepository;
import com.aidataanalyst.shared.tenant.TenantContext;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roles;

    public RoleController(RoleRepository roles) {
        this.roles = roles;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('roles:read')")
    public List<RoleResponse> list() {
        return roles.findByTenantIdOrderByCodeAsc(TenantContext.requireTenantId()).stream()
                .map(RoleResponse::from)
                .toList();
    }
}
