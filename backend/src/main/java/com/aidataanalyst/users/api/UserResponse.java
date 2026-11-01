package com.aidataanalyst.users.api;

import com.aidataanalyst.roles.domain.RoleCode;
import com.aidataanalyst.users.domain.UserStatus;
import com.aidataanalyst.users.infrastructure.UserEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        RoleCode roleCode,
        String roleName,
        List<String> permissions,
        Instant lastLoginAt,
        Instant createdAt) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.getRole().getCode(),
                user.getRole().getName(),
                List.copyOf(user.getRole().getPermissions()),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
