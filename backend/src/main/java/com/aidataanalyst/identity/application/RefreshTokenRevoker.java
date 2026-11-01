package com.aidataanalyst.identity.application;

import com.aidataanalyst.identity.infrastructure.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenRevoker {

    private final RefreshTokenRepository tokens;

    public RefreshTokenRevoker(RefreshTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeActive(UUID tenantId, UUID userId) {
        tokens.revokeActiveForUser(tenantId, userId, Instant.now());
    }
}
