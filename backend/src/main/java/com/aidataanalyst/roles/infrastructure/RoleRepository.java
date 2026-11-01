package com.aidataanalyst.roles.infrastructure;

import com.aidataanalyst.roles.domain.RoleCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByTenantIdAndCode(UUID tenantId, RoleCode code);

    List<RoleEntity> findByTenantIdOrderByCodeAsc(UUID tenantId);
}
