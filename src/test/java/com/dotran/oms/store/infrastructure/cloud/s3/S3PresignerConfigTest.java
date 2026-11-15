package com.dotran.oms.store.infrastructure.cloud.s3;

import com.dotran.oms.core.cloud.config.AwsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("S3PresignerConfig (mirrors oms-core S3Config)")
class S3PresignerConfigTest {

    private static AwsProperties aws(String endpoint, String accessKey, String secretKey) {
        AwsProperties properties = new AwsProperties();
        properties.setRegion("eu-west-1");
        properties.setEndpoint(endpoint);
        properties.setAccessKey(accessKey);
        properties.setSecretKey(secretKey);
        return properties;
    }

    private static URI presign(S3Presigner presigner) {
        try (presigner) {
            return URI.create(presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(1))
                    .putObjectRequest(r -> r.bucket("localdev-store-products").key("m/s/imports/f.xlsx"))
                    .build()).url().toString());
        }
    }

    @Test
    @DisplayName("localdev: LocalStack endpoint, path-style, test keys")
    void localdev() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("localdev");

        URI url = presign(new S3PresignerConfig().s3Presigner(env, aws("http://localhost:4566", null, null)));

        assertThat(url.getAuthority()).isEqualTo("localhost:4566");
        assertThat(url.getPath()).isEqualTo("/localdev-store-products/m/s/imports/f.xlsx");
        assertThat(url.getQuery()).contains("X-Amz-Credential=test/");
    }

    @Test
    @DisplayName("other profiles without an endpoint: the regional AWS host, virtual-hosted style, configured keys")
    void aws_noEndpoint() {
        URI url = presign(new S3PresignerConfig().s3Presigner(new MockEnvironment(), aws("", "AKIDEXAMPLE", "secret")));

        assertThat(url.getHost()).isEqualTo("localdev-store-products.s3.eu-west-1.amazonaws.com");
        assertThat(url.getPath()).isEqualTo("/m/s/imports/f.xlsx");
        assertThat(url.getQuery()).contains("X-Amz-Credential=AKIDEXAMPLE/");
    }
}
