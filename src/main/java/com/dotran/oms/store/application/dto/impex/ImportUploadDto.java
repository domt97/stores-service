package com.dotran.oms.store.application.dto.impex;

import java.time.Instant;
import java.util.Map;

/**
 * A presigned upload of one import file (BR-CAT-17): PUT the file, exactly {@code sizeBytes} long, to {@code uploadUrl}
 * with exactly {@code headers}, before {@code expiresAt}; then import it by {@code fileName}. The URL is a bearer link to
 * the store's folder: never log it ({@link #toString()} leaves it out).
 */
public record ImportUploadDto(String fileName, String uploadUrl, String method, Map<String, String> headers,
                              Instant expiresAt, long maxBytes) {

    @Override
    public String toString() {
        return "ImportUploadDto[fileName=" + fileName + ", method=" + method + ", headers=" + headers
                + ", expiresAt=" + expiresAt + ", maxBytes=" + maxBytes + "]";
    }
}
