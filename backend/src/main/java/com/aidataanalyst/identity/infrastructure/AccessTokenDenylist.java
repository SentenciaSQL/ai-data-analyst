package com.aidataanalyst.identity.infrastructure;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenDenylist {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenDenylist.class);
    private final StringRedisTemplate redis;

    public AccessTokenDenylist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redis.opsForValue().set(key(jti), "1", ttl);
        } catch (DataAccessException ex) {
            log.error("Could not record access-token revocation", ex);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(jti)));
        } catch (DataAccessException ex) {
            log.error("Could not check access-token revocation", ex);
            return false;
        }
    }

    private String key(String jti) {
        return "ada:auth:revoked:" + jti;
    }
}
