package com.dotran.example.store.application.service.storeconfig;

import com.dotran.example.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.mapper.StoreDataMapper;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.usecase.storeconfig.SettingStoreConfigUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.exception.StoreNotFoundException;
import com.dotran.example.store.domain.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class SettingStoreConfigService implements SettingStoreConfigUseCase {

    private final StoreRepository storeRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreDetailDto setupStoreConfig(UUID tenantIdString, UUID storeIdString, UpdateStoreConfigCmd cmd) {
        TenantId tenantId = TenantId.of(tenantIdString);
        StoreId storeId = StoreId.of(storeIdString);

        Store store = storeRepository.findByTenantIdAndStoreId(tenantId, storeId)
                .orElseThrow(StoreNotFoundException::new);

        store.updateConfig(
                cmd.isAutoAcceptOrder(),
                cmd.isAllowPreOrder(),
                cmd.getOpeningTime(),
                cmd.getClosingTime(),
                cmd.getTimeZone(),
                cmd.getCurrency(),
                cmd.getPreparationTimeMinutes(),
                cmd.getMaxOrdersPerDay());

        Store updatedStore = storeRepository.update(store);

        return storeDataMapper.toStoreDetailDto(updatedStore);
    }
}
