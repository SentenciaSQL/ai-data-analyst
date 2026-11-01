package com.aidataanalyst.users.application;

import com.aidataanalyst.audit.application.AuditService;
import com.aidataanalyst.identity.infrastructure.AuthGateway;
import com.aidataanalyst.identity.application.TextRules;
import com.aidataanalyst.roles.domain.RoleCode;
import com.aidataanalyst.roles.infrastructure.RoleEntity;
import com.aidataanalyst.roles.infrastructure.RoleRepository;
import com.aidataanalyst.shared.exception.ApiException;
import com.aidataanalyst.shared.tenant.TenantContext;
import com.aidataanalyst.users.api.CreateUserRequest;
import com.aidataanalyst.users.api.UpdateUserRequest;
import com.aidataanalyst.users.api.UserResponse;
import com.aidataanalyst.users.domain.UserStatus;
import com.aidataanalyst.users.infrastructure.UserEntity;
import com.aidataanalyst.users.infrastructure.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final AuthGateway authGateway;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(
            UserRepository users,
            RoleRepository roles,
            AuthGateway authGateway,
            PasswordEncoder passwordEncoder,
            AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.authGateway = authGateway;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findByTenantIdOrderByCreatedAtAsc(TenantContext.requireTenantId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UserEntity actor = requireActor(tenantId);
        String email = TextRules.email(request.email());
        if (authGateway.emailExists(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_IN_USE", "An account with this email already exists.");
        }
        RoleEntity role = requireRole(tenantId, request.roleCode());
        assertCanAssign(actor, role.getCode());
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        users.save(user);
        audit.record("USER_CREATED", "user", user.getId().toString(), Map.of("email", email, "role", role.getCode().name()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UserEntity actor = requireActor(tenantId);
        UserEntity target = users.findWithRoleByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "That person was not found."));
        RoleEntity nextRole = requireRole(tenantId, request.roleCode());
        assertCanChange(actor, target, nextRole.getCode(), request.status());
        target.setFirstName(request.firstName().trim());
        target.setLastName(request.lastName().trim());
        target.setRole(nextRole);
        target.setStatus(request.status());
        String action = request.status() == UserStatus.DISABLED ? "USER_DISABLED" : "USER_UPDATED";
        audit.record(action, "user", target.getId().toString(), Map.of(
                "email", target.getEmail(),
                "role", nextRole.getCode().name(),
                "status", request.status().name()));
        return UserResponse.from(target);
    }

    private UserEntity requireActor(UUID tenantId) {
        return users.findWithRoleByIdAndTenantId(TenantContext.requireUserId(), tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign in to continue."));
    }

    private RoleEntity requireRole(UUID tenantId, RoleCode code) {
        return roles.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "That role is not available."));
    }

    private void assertCanAssign(UserEntity actor, RoleCode role) {
        if (role == RoleCode.OWNER && actor.getRole().getCode() != RoleCode.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_REQUIRED", "Only an owner can grant the owner role.");
        }
    }

    private void assertCanChange(UserEntity actor, UserEntity target, RoleCode newRole, UserStatus newStatus) {
        boolean self = actor.getId().equals(target.getId());
        if (self && (newRole != actor.getRole().getCode() || newStatus != UserStatus.ACTIVE)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_CHANGE",
                    "You can't change your own role or disable your own account.");
        }
        boolean actorIsOwner = actor.getRole().getCode() == RoleCode.OWNER;
        if ((target.getRole().getCode() == RoleCode.OWNER || newRole == RoleCode.OWNER) && !actorIsOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_REQUIRED", "Only an owner can change an owner.");
        }
        boolean removingLastOwner = target.getRole().getCode() == RoleCode.OWNER
                && target.getStatus() == UserStatus.ACTIVE
                && (newRole != RoleCode.OWNER || newStatus != UserStatus.ACTIVE);
        if (removingLastOwner) {
            long owners = users.countByTenantIdAndRole_CodeAndStatus(
                    target.getTenantId(), RoleCode.OWNER, UserStatus.ACTIVE);
            if (owners <= 1) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "LAST_OWNER",
                        "The workspace needs at least one active owner.");
            }
        }
    }
}
