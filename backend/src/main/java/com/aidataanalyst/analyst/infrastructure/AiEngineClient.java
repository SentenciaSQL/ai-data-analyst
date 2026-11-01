package com.aidataanalyst.analyst.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ai-engine", url = "${ada.ai-engine.base-url}")
public interface AiEngineClient {

    @GetMapping("/health")
    EngineHealth health();

    record EngineHealth(String status, String service, String provider, boolean providerConfigured) {
    }
}
