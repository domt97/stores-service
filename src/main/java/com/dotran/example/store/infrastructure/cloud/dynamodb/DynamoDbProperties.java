package com.dotran.example.store.infrastructure.cloud.dynamodb;

import com.dotran.example.store.infrastructure.cloud.AwsProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class DynamoDbProperties extends AwsProperties {

    @Value("${aws.dynamodb.table.tenant-info:localdev_tenant_info}")
    private String tenantInfoTable;
}
