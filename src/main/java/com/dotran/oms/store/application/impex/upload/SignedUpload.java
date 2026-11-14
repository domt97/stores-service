package com.dotran.oms.store.application.impex.upload;

import java.time.Instant;
import java.util.Map;

/** A signed upload request: the client sends {@code method} to {@code url} with exactly {@code headers}. */
public record SignedUpload(String url, String method, Map<String, String> headers, Instant expiresAt) {

    @Override
    public String toString() {
        // the URL carries the signature: a bearer link, never printed
        return "SignedUpload[method=" + method + ", headers=" + headers + ", expiresAt=" + expiresAt + "]";
    }
}
