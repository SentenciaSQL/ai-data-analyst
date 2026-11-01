package com.aidataanalyst.identity.application;

import com.aidataanalyst.config.AppProperties;
import com.aidataanalyst.users.infrastructure.UserEntity;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AppProperties properties;
    private final JwtEncoder encoder;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        SecretKey key = new SecretKeySpec(properties.security().jwt().secret().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    public IssuedAccessToken issue(UserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.security().jwt().accessTokenTtl());
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.security().jwt().issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("tenant_id", user.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getCode().name())
                .claim("permissions", user.getRole().getPermissions())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt, properties.security().jwt().accessTokenTtl().toSeconds());
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(properties.security().jwt().secret().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }

    public record IssuedAccessToken(String token, Instant expiresAt, long expiresInSeconds) {
    }
}
