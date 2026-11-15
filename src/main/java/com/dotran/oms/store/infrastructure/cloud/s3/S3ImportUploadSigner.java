package com.dotran.oms.store.infrastructure.cloud.s3;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import com.dotran.oms.store.application.impex.upload.ImportUploadSigner;
import com.dotran.oms.store.application.impex.upload.SignedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Presigned PUT into the store's import folder (BR-CAT-16/17): the configured bucket and
 * {@link S3Properties#importKey} (the key the import reader reads). Content length, the .xlsx content type and the tag
 * {@code purpose=import-upload} (the lifecycle rule deletes tagged files after 7 days) are signed, so S3 rejects (403) a
 * different size, type, tag or key, and an expired URL.
 */
@Component
@RequiredArgsConstructor
public class S3ImportUploadSigner implements ImportUploadSigner {

    static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String UPLOAD_TAGGING = "purpose=import-upload";
    private static final String PUT = "PUT";

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public SignedUpload sign(MerchantId merchantId, StoreId storeId, ImportFileName fileName, long sizeBytes, Duration ttl) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(s3Properties.getStoreProductBucket())
                .key(s3Properties.importKey(merchantId, storeId, fileName))
                .contentLength(sizeBytes)
                .contentType(XLSX_CONTENT_TYPE)
                .tagging(UPLOAD_TAGGING)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(put)
                .build());
        // S3 expires the URL at X-Amz-Date (whole seconds) + X-Amz-Expires; the SDK's expiration keeps the sub-second
        // signing time, so truncate: the reported expiry is never later than the real one.
        return new SignedUpload(presigned.url().toString(), PUT, headersToSend(presigned.signedHeaders()),
                presigned.expiration().truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * The signed headers the client must send itself: all but {@code host} and {@code content-length}, which the browser
     * sets (and may not set from script). Names lower-case, one value each.
     */
    static Map<String, String> headersToSend(Map<String, List<String>> signedHeaders) {
        Map<String, String> headers = new TreeMap<>();
        signedHeaders.forEach((name, values) -> {
            String key = name.toLowerCase(Locale.ROOT);
            if (key.equals("host") || key.equals("content-length")) {
                return;
            }
            if (values.size() != 1) {
                throw new IllegalStateException("Signed header " + key + " has " + values.size() + " values");
            }
            headers.put(key, values.get(0));
        });
        return Map.copyOf(headers);
    }
}
