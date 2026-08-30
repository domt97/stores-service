package com.dotran.example.store.infrastructure.cloud.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Slf4j
public class S3Config {

    private static final String LOCALDEV = "localdev";

    @Autowired
    private Environment environment;

    @Autowired
    private S3Properties s3Properties;

    @Bean
    public S3Client s3Client() {
        // LocalStack
        if (environment.matchesProfiles(LOCALDEV)) {
            log.info("Init s3Client() for localdev env");
            return S3Client.builder()
                    .endpointOverride(URI.create(s3Properties.getEndpoint()))
                    .region(Region.of(s3Properties.getRegion()))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("test", "test")
                            )
                    )
                    .forcePathStyle(true)
                    .build();
        }

        // AWS
        var clientBuilder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()));

        // Optional endpoint override
        if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        // Optional static credentials
        if (s3Properties.getAccessKey() != null
                && !s3Properties.getAccessKey().isEmpty()
                && s3Properties.getSecretKey() != null
                && !s3Properties.getSecretKey().isEmpty()) {

            clientBuilder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    s3Properties.getAccessKey(),
                                    s3Properties.getSecretKey()
                            )
                    )
            );
        }

        return clientBuilder.build();
    }
}
