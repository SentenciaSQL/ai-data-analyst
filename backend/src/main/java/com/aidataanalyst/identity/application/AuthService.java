package com.aidataanalyst.identity.application;

import com.aidataanalyst.audit.application.AuditService;
import com.aidataanalyst.config.AppProperties;
import com.aidataanalyst.identity.api.AuthRequests.ChangePasswordRequest;
import com.aidataanalyst.identity.api.AuthRequests.LoginRequest;
import com.aidataanalyst.identity.api.AuthRequests.RegisterCompanyRequest;
import com.aidataanalyst.identity.api.AuthRequests.UpdateProfileRequest;
import com.aidataanalyst.identity.api.AuthResponse;
import com.aidataanalyst.identity.api.MeResponse;
import com.aidataanalyst.identity.infrastructure.AccessTokenDenylist;
import com.aidataanalyst.identity.infrastructure.AuthGateway;
import com.aidataanalyst.identity.infrastructure.AuthGateway.LoginUserRow;
import com.aidataanalyst.identity.infrastructure.AuthGateway.RefreshTokenRow;
import com.aidataanalyst.identity.infrastructure.RefreshTokenEntity;
import com.aidataanalyst.identity.infrastructure.RefreshTokenRepository;
import com.aidataanalyst.roles.domain.RoleCatalog;
import com.aidataanalyst.roles.domain.RoleCode;
import com.aidataanalyst.roles.infrastructure.RoleEntity;
import com.aidataanalyst.roles.infrastructure.RoleRepository;
import com.aidataanalyst.shared.exception.ApiException;
import com.aidataanalyst.shared.tenant.TenantContext;
import com.aidataanalyst.tenants.api.TenantResponse;
import com.aidataanalyst.tenants.domain.TenantStatus;
import com.aidataanalyst.tenants.infrastructure.TenantEntity;
import com.aidataanalyst.tenants.infrastructure.TenantRepository;
import com.aidataanalyst.users.api.UserResponse;
import com.aidataanalyst.users.domain.UserStatus;
import com.aidataanalyst.users.infrastructure.UserEntity;
import com.aidataanalyst.users.infrastructure.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final TenantRepository tenants;
    private final RoleRepository roles;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final RefreshTokenRevoker refreshTokenRevoker;
    private final AuthGateway authGateway;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccessTokenDenylist denylist;
    private final AuditService audit;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final String timingHash;

    public AuthService(
            TenantRepository tenants,
            RoleRepository roles,
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            RefreshTokenRevoker refreshTokenRevoker,
            AuthGateway authGateway,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccessTokenDenylist denylist,
            AuditService audit,
            AppProperties properties) {
        this.tenants = tenants;
        this.roles = roles;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.refreshTokenRevoker = refreshTokenRevoker;
        this.authGateway = authGateway;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.denylist = denylist;
        this.audit = audit;
        this.properties = properties;
        this.timingHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterCompanyRequest request, ClientRequestInfo client) {
        String email = TextRules.email(request.email());
        if (authGateway.emailExists(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_IN_USE", "An account with this email already exists.");
        }
        String slug = resolveSlug(request.slug(), request.companyName());
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(tenantId, userId);

        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);
        tenant.setName(request.companyName().trim());
        tenant.setSlug(slug);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPlanCode("trial");
        tenants.save(tenant);

        RoleEntity ownerRole = null;
        for (RoleCode code : RoleCode.values()) {
            RoleEntity role = new RoleEntity();
            role.setId(UUID.randomUUID());
            role.setTenantId(tenantId);
            role.setCode(code);
            role.setName(RoleCatalog.displayName(code));
            role.setDescription(RoleCatalog.description(code));
            role.setSystemRole(true);
            role.setPermissions(new ArrayList<>(RoleCatalog.permissionsFor(code)));
            roles.save(role);
            if (code == RoleCode.OWNER) {
                ownerRole = role;
            }
        }
        if (ownerRole == null) {
            throw new IllegalStateException("Owner role was not created.");
        }

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(ownerRole);
        users.save(user);
        audit.record("TENANT_REGISTERED", "tenant", tenantId.toString(), Map.of("slug", slug));
        return issueSession(user, tenant, client);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, ClientRequestInfo client) {
        String email = TextRules.email(request.email());
        LoginUserRow row = authGateway.findLoginUser(email).orElse(null);
        if (row == null) {
            passwordEncoder.matches(request.password(), timingHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), row.passwordHash())) {
            TenantContext.set(row.tenantId(), row.userId());
            audit.recordIndependently(
                    "USER_LOGIN_FAILED", "user", row.userId().toString(), Map.of("reason", "bad_password"));
            throw invalidCredentials();
        }
        if (!"ACTIVE".equals(row.userStatus()) || !"ACTIVE".equals(row.tenantStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "This account is not available.");
        }
        TenantContext.set(row.tenantId(), row.userId());
        UserEntity user = users.findWithRoleByIdAndTenantId(row.userId(), row.tenantId()).orElseThrow(this::invalidCredentials);
        user.setLastLoginAt(Instant.now());
        TenantEntity tenant = tenants.findById(row.tenantId()).orElseThrow(this::invalidCredentials);
        audit.record("USER_LOGIN", "user", user.getId().toString(), Map.of());
        return issueSession(user, tenant, client);
    }

    @Transactional
    public AuthResponse refresh(String rawToken, ClientRequestInfo client) {
        RefreshTokenRow row = authGateway.findRefreshToken(TokenHasher.sha256(rawToken)).orElseThrow(this::invalidRefresh);
        TenantContext.set(row.tenantId(), row.userId());
        if (row.revokedAt() != null) {
            refreshTokenRevoker.revokeActive(row.tenantId(), row.userId());
            throw invalidRefresh();
        }
        if (row.expiresAt().isBefore(Instant.now())) {
            throw invalidRefresh();
        }
        UserEntity user = users.findWithRoleByIdAndTenantId(row.userId(), row.tenantId()).orElseThrow(this::invalidRefresh);
        TenantEntity tenant = tenants.findById(row.tenantId()).orElseThrow(this::invalidRefresh);
        if (user.getStatus() != UserStatus.ACTIVE || tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "This account is not available.");
        }
        RefreshTokenEntity existing = refreshTokens.findById(row.tokenId()).orElseThrow(this::invalidRefresh);
        existing.setRevokedAt(Instant.now());
        return issueSession(user, tenant, client);
    }

    @Transactional
    public void logout(String refreshToken, String accessTokenId, Instant accessExpiresAt) {
        UUID userId = TenantContext.requireUserId();
        if (refreshToken != null && !refreshToken.isBlank()) {
            authGateway.findRefreshToken(TokenHasher.sha256(refreshToken)).ifPresent(row -> {
                if (userId.equals(row.userId())) {
                    refreshTokens.findById(row.tokenId()).ifPresent(token -> token.setRevokedAt(Instant.now()));
                }
            });
        }
        denylist.revoke(accessTokenId, accessExpiresAt);
        audit.record("USER_LOGOUT", "user", userId.toString(), Map.of());
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        UserEntity user = currentUser();
        TenantEntity tenant = currentTenant();
        return new MeResponse(UserResponse.from(user), TenantResponse.from(tenant));
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        UserEntity user = currentUser();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String accessTokenId, Instant accessExpiresAt) {
        UserEntity user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "The current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeActiveForUser(user.getTenantId(), user.getId(), Instant.now());
        denylist.revoke(accessTokenId, accessExpiresAt);
        audit.record("PASSWORD_CHANGED", "user", user.getId().toString(), Map.of());
    }

    private AuthResponse issueSession(UserEntity user, TenantEntity tenant, ClientRequestInfo client) {
        String rawRefresh = newRefreshToken();
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(UUID.randomUUID());
        token.setTenantId(user.getTenantId());
        token.setUserId(user.getId());
        token.setTokenHash(TokenHasher.sha256(rawRefresh));
        token.setExpiresAt(Instant.now().plus(properties.security().jwt().refreshTokenTtl()));
        token.setUserAgent(client.userAgent());
        token.setIpAddress(client.ipAddress());
        refreshTokens.save(token);
        JwtService.IssuedAccessToken access = jwtService.issue(user);
        return new AuthResponse(
                access.token(),
                rawRefresh,
                access.expiresInSeconds(),
                UserResponse.from(user),
                TenantResponse.from(tenant));
    }

    private String resolveSlug(String requested, String companyName) {
        if (requested != null && !requested.isBlank()) {
            String slug = requested.trim();
            if (authGateway.slugExists(slug)) {
                throw new ApiException(HttpStatus.CONFLICT, "SLUG_IN_USE", "This workspace URL is already taken.");
            }
            return slug;
        }
        String base = TextRules.slugFromName(companyName);
        String candidate = base;
        for (int suffix = 2; suffix < 100; suffix++) {
            if (!authGateway.slugExists(candidate)) {
                return candidate;
            }
            String ending = "-" + suffix;
            String trimmed = base;
            if (trimmed.length() + ending.length() > 48) {
                trimmed = trimmed.substring(0, 48 - ending.length()).replaceAll("-+$", "");
            }
            candidate = trimmed + ending;
        }
        throw new ApiException(HttpStatus.CONFLICT, "SLUG_IN_USE", "This workspace URL is already taken.");
    }

    private UserEntity currentUser() {
        return users.findWithRoleByIdAndTenantId(TenantContext.requireUserId(), TenantContext.requireTenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in to continue."));
    }

    private TenantEntity currentTenant() {
        return tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Workspace not found."));
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return "rt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
    }

    private ApiException invalidRefresh() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Sign in again.");
    }
}
