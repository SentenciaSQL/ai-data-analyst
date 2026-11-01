package com.aidataanalyst.identity.api;

import com.aidataanalyst.identity.api.AuthRequests.ChangePasswordRequest;
import com.aidataanalyst.identity.api.AuthRequests.LoginRequest;
import com.aidataanalyst.identity.api.AuthRequests.LogoutRequest;
import com.aidataanalyst.identity.api.AuthRequests.RefreshRequest;
import com.aidataanalyst.identity.api.AuthRequests.RegisterCompanyRequest;
import com.aidataanalyst.identity.api.AuthRequests.UpdateProfileRequest;
import com.aidataanalyst.identity.application.AuthService;
import com.aidataanalyst.identity.application.ClientRequestInfo;
import com.aidataanalyst.shared.exception.ApiException;
import com.aidataanalyst.users.api.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterCompanyRequest request, HttpServletRequest httpRequest) {
        return auth.register(request, ClientRequestInfo.from(httpRequest));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return auth.login(request, ClientRequestInfo.from(httpRequest));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return auth.refresh(request.refreshToken(), ClientRequestInfo.from(httpRequest));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest request) {
        Jwt jwt = currentJwt();
        String refreshToken = request == null ? null : request.refreshToken();
        auth.logout(refreshToken, jwt.getId(), jwt.getExpiresAt());
    }

    @GetMapping("/me")
    public MeResponse me() {
        return auth.me();
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return auth.updateProfile(request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Jwt jwt = currentJwt();
        auth.changePassword(request, jwt.getId(), jwt.getExpiresAt());
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in to continue.");
    }
}
