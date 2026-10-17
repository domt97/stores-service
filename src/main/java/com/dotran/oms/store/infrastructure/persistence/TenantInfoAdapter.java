package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.cloud.dynamodb.DynamoDbTenantInfoRepository;
import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.application.repository.TenantRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class TenantInfoAdapter implements TenantRepository {

    private final DynamoDbTenantInfoRepository dynamoDbTenantInfoRepository;

    @Override
    public Optional<TenantInfo> findByTenantId(TenantId tenantId) {
        return dynamoDbTenantInfoRepository.findByTenantId(tenantId);
    }
}
