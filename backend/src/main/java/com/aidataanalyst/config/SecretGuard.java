package com.aidataanalyst.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SecretGuard implements ApplicationRunner {

    private final AppProperties properties;

    public SecretGuard(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String secret = properties.security().jwt().secret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters.");
        }
        if (!"production".equalsIgnoreCase(properties.environment())) {
            return;
        }
        if (secret.contains("dev-only") || properties.aiEngine().apiKey().contains("dev-only")) {
            throw new IllegalStateException("Refusing development secrets while ADA_ENV=production.");
        }
    }
}
