package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.command.store.CloseStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.exception.StoreAlreadyClosedException;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseStoreService")
class CloseStoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private CloseStoreService closeStoreService;

    private MerchantId merchantId = MerchantId.newMerchantId();
    private StoreId storeId = StoreId.newStoreId();

    private CloseStoreCmd cmd() {
        return CloseStoreCmd.builder().merchantId(merchantId).storeId(storeId).build();
    }

    private Store buildStore(StoreStatus status) {
        return Store.builder()
                .id(storeId)
                .merchantId(merchantId)
                .name("Test Store")
                .status(status)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("throws StoreNotFoundException when the store does not exist")
    void throwsException_whenStoreNotFound() {
        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> closeStoreService.close(cmd()))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    @DisplayName("throws StoreAlreadyClosedException when the store is already CLOSED")
    void throwsException_whenAlreadyClosed() {
        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId))
                .thenReturn(Optional.of(buildStore(StoreStatus.CLOSED)));

        assertThatThrownBy(() -> closeStoreService.close(cmd()))
                .isInstanceOf(StoreAlreadyClosedException.class);
    }

    @Test
    @DisplayName("closes an ACTIVE store and returns the DTO")
    void closesStore_andReturnsDto() {
        Store activeStore = buildStore(StoreStatus.ACTIVE);
        Store closedStore = buildStore(StoreStatus.CLOSED);

        StoreDetailDto expectedDto = StoreDetailDto.builder()
                .id(UUID.randomUUID())
                .status(StoreStatus.CLOSED)
                .build();

        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId))
                .thenReturn(Optional.of(activeStore));
        when(storeRepository.close(any(Store.class))).thenReturn(closedStore);
        when(storeDataMapper.toStoreDetailDto(closedStore)).thenReturn(expectedDto);

        StoreDetailDto result = closeStoreService.close(cmd());

        assertThat(result.getStatus()).isEqualTo(StoreStatus.CLOSED);
    }
}
