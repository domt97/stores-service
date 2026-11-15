package com.dotran.oms.store.infrastructure.cloud.s3;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import com.dotran.oms.store.application.impex.upload.SignedUpload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real SDK presigner, offline (presigning is a local signature; no S3 call). */
@DisplayName("S3ImportUploadSigner")
class S3ImportUploadSignerTest {

    private static final MerchantId MERCHANT = MerchantId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final StoreId STORE = StoreId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final ImportFileName FILE = ImportFileName.of("33333333-3333-3333-3333-333333333333.xlsx");
    private static final String KEY =
            "11111111-1111-1111-1111-111111111111/22222222-2222-2222-2222-222222222222/imports/33333333-3333-3333-3333-333333333333.xlsx";

    private static S3Presigner presigner;
    private static S3ImportUploadSigner signer;

    @BeforeAll
    static void setUp() {
        presigner = S3Presigner.builder()
                .region(Region.of("eu-west-1"))
                .endpointOverride(URI.create("http://localhost:4566"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        signer = new S3ImportUploadSigner(presigner, new S3Properties("localdev-store-products"));
    }

    @AfterAll
    static void tearDown() {
        presigner.close();
    }

    private static Map<String, String> query(String url) {
        return Arrays.stream(URI.create(url).getRawQuery().split("&"))
                .map(p -> p.split("=", 2))
                .collect(Collectors.toMap(p -> p[0], p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("PUT to the configured bucket and the store's import key, path-style on the endpoint, valid for the ttl")
    void urlLocationAndExpiry() {
        SignedUpload upload = signer.sign(MERCHANT, STORE, FILE, 1234, Duration.ofMinutes(15));

        URI url = URI.create(upload.url());
        assertThat(upload.method()).isEqualTo("PUT");
        assertThat(url.getScheme() + "://" + url.getAuthority()).isEqualTo("http://localhost:4566");
        assertThat(url.getRawPath()).isEqualTo("/localdev-store-products/" + KEY);
        Map<String, String> query = query(upload.url());
        assertThat(query).containsEntry("X-Amz-Expires", "900");
        assertThat(query.keySet()).noneMatch(k -> k.toLowerCase().startsWith("x-amz-checksum")
                || k.equalsIgnoreCase("x-amz-sdk-checksum-algorithm"));
        Instant signedAt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
                .parse(query.get("X-Amz-Date"), Instant::from);
        assertThat(upload.expiresAt()).isEqualTo(signedAt.plusSeconds(900));
    }

    @Test
    @DisplayName("size, xlsx content type and cleanup tag are signed; the client is told exactly content-type and x-amz-tagging")
    void signedHeaders() {
        SignedUpload upload = signer.sign(MERCHANT, STORE, FILE, 1234, Duration.ofMinutes(15));

        assertThat(List.of(query(upload.url()).get("X-Amz-SignedHeaders").split(";")))
                .containsExactlyInAnyOrder("content-length", "content-type", "host", "x-amz-tagging");
        assertThat(upload.headers()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "content-type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "x-amz-tagging", "purpose=import-upload"));
        assertThat(upload.toString()).doesNotContain("X-Amz-Signature").doesNotContain("localhost");
    }

    @Test
    @DisplayName("headers to send: host and content-length dropped, names lower-cased; several values fail fast")
    void headersToSend() {
        assertThat(S3ImportUploadSigner.headersToSend(Map.of("Host", List.of("h"), "Content-Length", List.of("5"),
                "Content-Type", List.of("t"), "X-Amz-Tagging", List.of("purpose=import-upload"))))
                .containsExactlyInAnyOrderEntriesOf(Map.of("content-type", "t", "x-amz-tagging", "purpose=import-upload"));
        assertThatThrownBy(() -> S3ImportUploadSigner.headersToSend(Map.of("x-amz-meta", List.of("a", "b"))))
                .isInstanceOf(IllegalStateException.class);
    }
}
