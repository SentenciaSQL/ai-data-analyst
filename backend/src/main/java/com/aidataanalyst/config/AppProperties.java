package com.aidataanalyst.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ada")
public record AppProperties(
        String environment,
        Security security,
        AiEngine aiEngine,
        Cors cors) {

    public record Security(Jwt jwt) {
    }

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record AiEngine(String baseUrl, String apiKey) {
    }

    public record Cors(String allowedOrigins) {
    }
}
