package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.command.store.GetStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetStoreService")
class GetStoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private GetStoreService getStoreService;

    @Test
    @DisplayName("throws StoreNotFoundException when store does not exist")
    void throwsException_whenStoreNotFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        GetStoreCmd cmd = GetStoreCmd.builder().merchantId(merchantId).storeId(storeId).build();

        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreService.getStoreByMerchantIdAndStoreId(cmd))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    @DisplayName("returns StoreDetailDto when store exists")
    void returnsDto_whenStoreFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        GetStoreCmd cmd = GetStoreCmd.builder().merchantId(merchantId).storeId(storeId).build();

        Store store = mock(Store.class);
        StoreDetailDto expectedDto = StoreDetailDto.builder().id(UUID.randomUUID()).name("My Store").build();

        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.of(store));
        when(storeDataMapper.toStoreDetailDto(store)).thenReturn(expectedDto);

        StoreDetailDto result = getStoreService.getStoreByMerchantIdAndStoreId(cmd);

        assertThat(result).isEqualTo(expectedDto);
    }
}
