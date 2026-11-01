package com.aidataanalyst;

import com.aidataanalyst.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(AppProperties.class)
public class AiDataAnalystApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDataAnalystApplication.class, args);
    }
}
