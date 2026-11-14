package com.dotran.oms.store.infrastructure.cloud.s3;

import com.dotran.oms.core.cloud.config.AwsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Presigner for import uploads (BR-CAT-17). It mirrors the branches of oms-core {@code S3Config.s3Client()} so that a
 * presigned URL points where the service's own S3 client goes: localdev → LocalStack endpoint, {@code test} keys,
 * path-style; otherwise region, optional endpoint and static keys (else the default credential chain). Kept here, not in
 * oms-core, because only stores-service presigns (system rule 6) — keep it in sync with {@code S3Config}.
 */
@Configuration(proxyBeanMethods = false)
public class S3PresignerConfig {

    private static final String LOCALDEV = "localdev";

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(Environment environment, AwsProperties awsProperties) {
        S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(awsProperties.getRegion()));
        if (environment.matchesProfiles(LOCALDEV)) {
            return builder
                    .endpointOverride(URI.create(awsProperties.getEndpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        if (hasText(awsProperties.getEndpoint())) {
            builder.endpointOverride(URI.create(awsProperties.getEndpoint()));
        }
        if (hasText(awsProperties.getAccessKey()) && hasText(awsProperties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())));
        }
        return builder.build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
