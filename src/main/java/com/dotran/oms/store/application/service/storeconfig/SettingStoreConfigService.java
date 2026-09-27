package com.dotran.oms.store.application.service.storeconfig;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.storeconfig.SettingStoreConfigUseCase;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.Store;
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
