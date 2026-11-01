package com.aidataanalyst.shared.tenant;

import com.aidataanalyst.identity.infrastructure.AccessTokenDenylist;
import com.aidataanalyst.shared.exception.ErrorResponses;
import com.aidataanalyst.tenants.domain.TenantStatus;
import com.aidataanalyst.tenants.infrastructure.TenantRepository;
import com.aidataanalyst.users.domain.UserStatus;
import com.aidataanalyst.users.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantContextFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final TenantRepository tenants;
    private final AccessTokenDenylist denylist;

    public TenantContextFilter(UserRepository users, TenantRepository tenants, AccessTokenDenylist denylist) {
        this.users = users;
        this.tenants = tenants;
        this.denylist = denylist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                if (denylist.isRevoked(jwt.getId())) {
                    ErrorResponses.write(response, 401, "ACCESS_TOKEN_REVOKED", "Sign in again.", request.getRequestURI());
                    return;
                }
                UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));
                UUID userId = UUID.fromString(jwt.getSubject());
                TenantContext.set(tenantId, userId);
                var user = users.findByIdAndTenantId(userId, tenantId).orElse(null);
                if (user == null || user.getStatus() != UserStatus.ACTIVE || !tenantId.equals(user.getTenantId())) {
                    ErrorResponses.write(response, 401, "ACCOUNT_UNAVAILABLE", "This account is not available.", request.getRequestURI());
                    return;
                }
                var tenant = tenants.findById(tenantId).orElse(null);
                if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
                    ErrorResponses.write(response, 403, "TENANT_UNAVAILABLE", "This workspace is not available.", request.getRequestURI());
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
