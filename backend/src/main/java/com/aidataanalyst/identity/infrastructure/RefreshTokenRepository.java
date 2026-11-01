package com.aidataanalyst.identity.infrastructure;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshTokenEntity token
            set token.revokedAt = :revokedAt
            where token.tenantId = :tenantId
              and token.userId = :userId
              and token.revokedAt is null
            """)
    int revokeActiveForUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt);
}
