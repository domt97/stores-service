package com.dotran.example.store.infrastructure.cloud;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    protected String region;
    protected String endpoint;
    protected String accessKey;
    protected String secretKey;
}
