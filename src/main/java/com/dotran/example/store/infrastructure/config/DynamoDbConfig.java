package com.dotran.example.store.infrastructure.config;

import com.dotran.example.store.infrastructure.cloud.dynamodb.TenantInfoItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    @Value("${app.dynamodb.region:eu-west-1}")
    private String region;

    @Value("${app.dynamodb.access-key:}")
    private String accessKey;

    @Value("${app.dynamodb.secret-key:}")
    private String secretKey;

    @Value("${app.dynamodb.table.tenant-info:localdev_tenant_info}")
    private String tenantInfoTableName;

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
                .region(Region.of(region));

        // Use endpoint override if provided (e.g., for local testing with LocalStack)
        if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isEmpty()) {
            clientBuilder.endpointOverride(java.net.URI.create(dynamoDbEndpoint));
        }

        // Use static credentials if provided (e.g., for local development)
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            clientBuilder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
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
        return enhancedClient.table(tenantInfoTableName, TableSchema.fromBean(TenantInfoItem.class));
    }
}
