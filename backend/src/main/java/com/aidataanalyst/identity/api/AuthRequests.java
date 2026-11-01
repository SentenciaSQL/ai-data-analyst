package com.aidataanalyst.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {
    }

    public record RegisterCompanyRequest(
            @NotBlank @Size(max = 200) String companyName,
            @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Use lowercase letters, numbers, and hyphens.")
            @Size(min = 3, max = 48) String slug,
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank
            @Size(min = 10, max = 128)
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).{10,128}$",
                    message = "Use at least 10 characters with a letter and a number.")
            String password) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank
            @Size(min = 10, max = 128)
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).{10,128}$",
                    message = "Use at least 10 characters with a letter and a number.")
            String newPassword) {
    }
}
