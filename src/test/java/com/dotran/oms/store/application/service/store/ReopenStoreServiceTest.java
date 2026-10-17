package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.application.command.store.ReopenStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.domain.enums.StoreStatus;
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
@DisplayName("ReopenStoreService")
class ReopenStoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private ReopenStoreService reopenStoreService;

    private TenantId tenantId = TenantId.newTenantId();
    private StoreId storeId = StoreId.newStoreId();

    private ReopenStoreCmd cmd() {
        return ReopenStoreCmd.builder().tenantId(tenantId).storeId(storeId).build();
    }

    private Store buildStore(StoreStatus status) {
        return Store.builder()
                .id(storeId)
                .tenantId(tenantId)
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
        when(storeRepository.findByTenantIdAndStoreId(tenantId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reopenStoreService.reopen(cmd()))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    @DisplayName("throws BusinessException when store is not CLOSED")
    void throwsException_whenStoreNotClosed() {
        when(storeRepository.findByTenantIdAndStoreId(tenantId, storeId))
                .thenReturn(Optional.of(buildStore(StoreStatus.ACTIVE)));

        assertThatThrownBy(() -> reopenStoreService.reopen(cmd()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("reopens a CLOSED store and returns the DTO")
    void reopensStore_andReturnsDto() {
        Store closedStore = buildStore(StoreStatus.CLOSED);
        Store activeStore = buildStore(StoreStatus.ACTIVE);

        StoreDetailDto expectedDto = StoreDetailDto.builder()
                .id(UUID.randomUUID())
                .status(StoreStatus.ACTIVE)
                .build();

        when(storeRepository.findByTenantIdAndStoreId(tenantId, storeId))
                .thenReturn(Optional.of(closedStore));
        when(storeRepository.reopen(any(Store.class))).thenReturn(activeStore);
        when(storeDataMapper.toStoreDetailDto(activeStore)).thenReturn(expectedDto);

        StoreDetailDto result = reopenStoreService.reopen(cmd());

        assertThat(result.getStatus()).isEqualTo(StoreStatus.ACTIVE);
    }
}
