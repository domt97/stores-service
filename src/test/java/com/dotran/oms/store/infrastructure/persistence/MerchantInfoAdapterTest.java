package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.cloud.dynamodb.DynamoDbMerchantInfoRepository;
import com.dotran.oms.core.domain.MerchantInfo;
import com.dotran.oms.core.domain.id.MerchantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantInfoAdapter")
class MerchantInfoAdapterTest {

    @Mock
    private DynamoDbMerchantInfoRepository dynamoDbMerchantInfoRepository;

    @InjectMocks
    private MerchantInfoAdapter adapter;

    @Test
    @DisplayName("findByMerchantId: returns merchant info when found")
    void findByMerchantId_returnsMerchantInfo_whenFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        MerchantInfo merchantInfo = mock(MerchantInfo.class);

        when(dynamoDbMerchantInfoRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(merchantInfo));

        Optional<MerchantInfo> result = adapter.findByMerchantId(merchantId);

        assertThat(result).contains(merchantInfo);
    }

    @Test
    @DisplayName("findByMerchantId: returns empty when merchant not found")
    void findByMerchantId_returnsEmpty_whenNotFound() {
        MerchantId merchantId = MerchantId.newMerchantId();

        when(dynamoDbMerchantInfoRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());

        Optional<MerchantInfo> result = adapter.findByMerchantId(merchantId);

        assertThat(result).isEmpty();
    }
}
