package com.aidataanalyst.users.api;

import com.aidataanalyst.roles.domain.RoleCode;
import com.aidataanalyst.users.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotNull RoleCode roleCode,
        @NotNull UserStatus status) {
}
