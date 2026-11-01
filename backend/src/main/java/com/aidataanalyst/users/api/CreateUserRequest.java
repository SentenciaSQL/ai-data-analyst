package com.aidataanalyst.users.api;

import com.aidataanalyst.roles.domain.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotBlank
        @Size(min = 10, max = 128)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{10,128}$",
                message = "Use at least 10 characters with a letter and a number.")
        String password,
        @NotNull RoleCode roleCode) {
}
