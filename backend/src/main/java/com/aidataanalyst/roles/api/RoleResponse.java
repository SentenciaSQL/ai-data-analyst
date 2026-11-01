package com.aidataanalyst.roles.api;

import com.aidataanalyst.roles.domain.RoleCode;
import com.aidataanalyst.roles.infrastructure.RoleEntity;
import java.util.List;

public record RoleResponse(
        RoleCode code,
        String name,
        String description,
        boolean systemRole,
        List<String> permissions) {

    public static RoleResponse from(RoleEntity role) {
        return new RoleResponse(
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                List.copyOf(role.getPermissions()));
    }
}
