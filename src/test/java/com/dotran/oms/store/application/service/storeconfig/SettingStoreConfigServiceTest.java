package com.dotran.oms.store.application.service.storeconfig;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.valueobject.StoreConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingStoreConfigService")
class SettingStoreConfigServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private SettingStoreConfigService settingStoreConfigService;

    private Store storeWithConfig() {
        StoreConfig config = StoreConfig.builder()
                .autoAcceptOrder(false)
                .allowPreOrder(false)
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .timeZone("UTC")
                .currency("USD")
                .preparationTimeMinutes(30)
                .maxOrdersPerDay(100)
                .build();

        return Store.builder()
                .id(StoreId.newStoreId())
                .tenantId(TenantId.newTenantId())
                .name("Test Store")
                .status(StoreStatus.ACTIVE)
                .config(config)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("throws StoreNotFoundException when store does not exist")
    void throwsException_whenStoreNotFound() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UpdateStoreConfigCmd cmd = UpdateStoreConfigCmd.builder()
                .autoAcceptOrder(true)
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .timeZone("UTC")
                .currency("USD")
                .build();

        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingStoreConfigService.setupStoreConfig(tenantId, storeId, cmd))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    @DisplayName("updates store config and returns updated DTO")
    void updatesConfig_andReturnsDto() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = storeWithConfig();

        UpdateStoreConfigCmd cmd = UpdateStoreConfigCmd.builder()
                .autoAcceptOrder(true)
                .allowPreOrder(true)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(22, 0))
                .timeZone("UTC")
                .currency("EUR")
                .preparationTimeMinutes(15)
                .maxOrdersPerDay(200)
                .build();

        StoreDetailDto expectedDto = StoreDetailDto.builder()
                .id(UUID.randomUUID())
                .name("Test Store")
                .status(StoreStatus.ACTIVE)
                .build();

        when(storeRepository.findByTenantIdAndStoreId(any(), any())).thenReturn(Optional.of(store));
        when(storeRepository.update(any(Store.class))).thenReturn(store);
        when(storeDataMapper.toStoreDetailDto(store)).thenReturn(expectedDto);

        StoreDetailDto result = settingStoreConfigService.setupStoreConfig(tenantId, storeId, cmd);

        assertThat(result).isEqualTo(expectedDto);
        verify(storeRepository).update(store);
    }
}
