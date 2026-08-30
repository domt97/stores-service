package com.dotran.example.store.infrastructure.cloud.dynamodb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
@Slf4j
public class DynamoDbConfig {

    private static final String LOCALDEV = "localdev";

    @Autowired
    private Environment environment;

    @Autowired
    private DynamoDbProperties dynamoDbProperties;

    @Bean
    public DynamoDbClient dynamoDbClient() {

        // localdev
        if (environment.matchesProfiles(LOCALDEV)) {
            log.info("Init dynamoDbClient() for localdev env");
            return DynamoDbClient.builder()
                    .endpointOverride(URI.create("http://localhost:4566"))
                    .region(Region.EU_WEST_1)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("test", "test")
                            )
                    )
                    .build();
        }
        var clientBuilder = DynamoDbClient.builder()
                .region(Region.of(dynamoDbProperties.getRegion()));

        // Use endpoint override if provided (e.g., for local testing with LocalStack)
        if (dynamoDbProperties.getEndpoint() != null && !dynamoDbProperties.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(java.net.URI.create(dynamoDbProperties.getEndpoint()));
        }

        // Use static credentials if provided (e.g., for local development)
        if (dynamoDbProperties.getAccessKey() != null && !dynamoDbProperties.getAccessKey().isEmpty()
                && dynamoDbProperties.getSecretKey() != null && !dynamoDbProperties.getSecretKey().isEmpty()) {
            clientBuilder.credentialsProvider(
                StaticCredentialsProvider.create(
                        AwsBasicCredentials
                                .create(dynamoDbProperties.getAccessKey(), dynamoDbProperties.getSecretKey()))
            );
        }

        return clientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<TenantInfoItem> tenantInfoTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(dynamoDbProperties.getTenantInfoTable(), TableSchema.fromBean(TenantInfoItem.class));
    }
}
