package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.cloud.dynamodb.DynamoDbMerchantInfoRepository;
import com.dotran.oms.core.domain.MerchantInfo;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class MerchantInfoAdapter implements MerchantRepository {

    private final DynamoDbMerchantInfoRepository dynamoDbMerchantInfoRepository;

    @Override
    public Optional<MerchantInfo> findByMerchantId(MerchantId merchantId) {
        return dynamoDbMerchantInfoRepository.findByMerchantId(merchantId);
    }
}
