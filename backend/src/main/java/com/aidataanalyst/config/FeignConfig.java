package com.aidataanalyst.config;

import feign.Request;
import feign.RequestInterceptor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor aiEngineApiKey(AppProperties properties) {
        return template -> template.header("X-Internal-Api-Key", properties.aiEngine().apiKey());
    }

    @Bean
    public Request.Options aiEngineOptions() {
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
