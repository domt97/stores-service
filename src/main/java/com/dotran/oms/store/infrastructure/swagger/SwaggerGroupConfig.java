package com.dotran.oms.store.infrastructure.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerGroupConfig {

    @Bean
    public GroupedOpenApi storeApi() {
        return GroupedOpenApi.builder()
                .group("store")
                .packagesToScan(
                        "com.dotran.oms.store.infrastructure.rest"
                )
                .build();
    }
}
