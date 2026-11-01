package com.aidataanalyst.users.infrastructure;

import com.aidataanalyst.roles.domain.RoleCode;
import com.aidataanalyst.users.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = "role")
    Optional<UserEntity> findWithRoleByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = "role")
    List<UserEntity> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, UserStatus status);

    long countByTenantIdAndRole_CodeAndStatus(UUID tenantId, RoleCode code, UserStatus status);
}
