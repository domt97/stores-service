package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.cloud.dynamodb.DynamoDbTenantInfoRepository;
import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.TenantId;
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
@DisplayName("TenantInfoAdapter")
class TenantInfoAdapterTest {

    @Mock
    private DynamoDbTenantInfoRepository dynamoDbTenantInfoRepository;

    @InjectMocks
    private TenantInfoAdapter adapter;

    @Test
    @DisplayName("findByTenantId: returns tenant info when found")
    void findByTenantId_returnsTenantInfo_whenFound() {
        TenantId tenantId = TenantId.newTenantId();
        TenantInfo tenantInfo = mock(TenantInfo.class);

        when(dynamoDbTenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenantInfo));

        Optional<TenantInfo> result = adapter.findByTenantId(tenantId);

        assertThat(result).contains(tenantInfo);
    }

    @Test
    @DisplayName("findByTenantId: returns empty when tenant not found")
    void findByTenantId_returnsEmpty_whenNotFound() {
        TenantId tenantId = TenantId.newTenantId();

        when(dynamoDbTenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        Optional<TenantInfo> result = adapter.findByTenantId(tenantId);

        assertThat(result).isEmpty();
    }
}
