package com.dotran.example.store.infrastructure.rest.logging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private final RequestLoggingProperties props;

    private Set<String> sensitiveHeadersLower;
    private Set<String> sensitiveQueryParamsLower;

    @PostConstruct
    private void init() {
        sensitiveHeadersLower = props.getSensitiveHeaders().stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toSet());
        sensitiveQueryParamsLower = props.getSensitiveQueryParams().stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!props.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Log request metadata before dispatching to controller/handlers
        try {
            logRequestMetadata(wrappedRequest);
        } catch (Exception e) {
            log.warn("Failed to log request metadata", e);
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                logResponseMetadata(wrappedResponse, duration);
            } catch (Exception e) {
                log.warn("Failed to log response metadata", e);
            }
            // copy response body back to the original response output stream
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestMetadata(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String fullUri = buildRedactedRequestUri(request);

        Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(h -> h, h -> redactHeaderValue(h, request.getHeader(h)), (a, b) -> b, LinkedHashMap::new));

        log.info("Incoming Request: method={} uri={} headers={}", method, fullUri, headers);
    }

    private void logResponseMetadata(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        Map<String, String> headers = response.getHeaderNames().stream()
                .collect(Collectors.toMap(h -> h, h -> redactHeaderValue(h, response.getHeader(h)), (a, b) -> b, LinkedHashMap::new));

        log.info("Outgoing Response: status={} durationMs={} headers={}", status, duration, headers);
    }

    private String redactHeaderValue(String headerName, String value) {
        if (headerName == null) return value;
        if (value == null) return null;
        if (sensitiveHeadersLower != null && sensitiveHeadersLower.contains(headerName.toLowerCase())) return "[REDACTED]";
        return value;
    }

    private String buildRedactedRequestUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.isEmpty()) return uri;
        StringBuilder sb = new StringBuilder(uri).append("?");
        java.util.StringJoiner joiner = new java.util.StringJoiner("&");
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String name = e.getKey();
            String[] values = e.getValue();
            String valueStr = "";
            if (values != null && values.length > 0) {
                if (sensitiveQueryParamsLower != null && sensitiveQueryParamsLower.contains(name.toLowerCase())) {
                    valueStr = "[REDACTED]";
                } else {
                    valueStr = String.join(",", values);
                }
            }
            joiner.add(name + "=" + valueStr);
        }
        sb.append(joiner.toString());
        return sb.toString();
    }
}
