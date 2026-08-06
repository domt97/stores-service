package com.dotran.example.store.infrastructure.cloud.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Setter
@Getter
@NoArgsConstructor
@DynamoDbBean
public class TenantSettingsItem {

    private String timeZone;

    private String currency;

    private String locale;

}
