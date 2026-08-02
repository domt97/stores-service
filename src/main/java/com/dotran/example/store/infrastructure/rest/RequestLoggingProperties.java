package com.dotran.example.store.infrastructure.rest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.logging.http")
@Getter
@Setter
public class RequestLoggingProperties {

    private boolean enabled = true;
    private boolean logRequestMetadata = true;
    private boolean logResponseMetadata = true;

    private List<String> sensitiveHeaders = List.of(
            "authorization", "cookie", "set-cookie", "proxy-authorization", "x-api-key", "x-csrf-token"
    );

    private List<String> sensitiveQueryParams = List.of(
            "token", "access_token", "api_key", "auth", "password", "refresh_token"
    );

    private int maxPayloadLogBytes = 1024 * 10; // 10KB
    private int maxContentLengthToLog = 1024 * 50; // 50KB

    private List<String> skipContentTypes = List.of("multipart/", "image/", "video/", "application/octet-stream");
}
