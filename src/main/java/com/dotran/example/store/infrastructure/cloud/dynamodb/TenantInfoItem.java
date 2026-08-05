package com.dotran.example.store.infrastructure.cloud.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@DynamoDbBean
public class TenantInfoItem {

    private UUID pk;

    private String code;

    private String name;

    private String status;

    private TenantSettingsItem settings;

    private Instant createdAt;

    private Instant updatedAt;

    @DynamoDbPartitionKey
    public UUID getPk() {
        return pk;
    }
}
