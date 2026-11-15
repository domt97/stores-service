package com.dotran.oms.store.infrastructure.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Presigned upload of one import file: PUT the file to uploadUrl with exactly these headers before "
        + "expiresAt, then import it by fileName")
public record ImportUploadResponse(
        @Schema(description = "Name to send to the import, in the store's import folder") String fileName,
        @Schema(description = "Presigned URL (a bearer link: do not log or share)") String uploadUrl,
        @Schema(description = "Always PUT") String method,
        @Schema(description = "Headers to send with the PUT, exactly as given") Map<String, String> headers,
        @Schema(description = "When the URL stops working") Instant expiresAt,
        @Schema(description = "Largest file accepted, in bytes (BR-CAT-13)") long maxBytes) {

    @Override
    public String toString() {
        return "ImportUploadResponse[fileName=" + fileName + ", expiresAt=" + expiresAt + "]";
    }
}
