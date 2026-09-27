package com.dotran.oms.store.application.service.storeconfig;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.storeconfig.SettingStoreBusinessHourUseCase;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.BusinessHour;
import com.dotran.oms.store.domain.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotran.oms.store.common.constants.Constants.ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG;

@UseCase
@RequiredArgsConstructor
public class SettingBusinessHourService implements SettingStoreBusinessHourUseCase {

    private final StoreRepository storeRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreDetailDto setupBusinessHour(UUID tenantIdString, UUID storeIdString, List<UpdateBusinessHourCmd> updateBusinessHourCmds) {
        TenantId tenantId = TenantId.of(tenantIdString);
        StoreId storeId = StoreId.of(storeIdString);

        Store store = storeRepository.findByTenantIdAndStoreId(tenantId, storeId)
                .orElseThrow(StoreNotFoundException::new);

        Map<Long, UpdateBusinessHourCmd> updateBusinessHourCmdMap = updateBusinessHourCmds.stream()
                .collect(Collectors.toMap(UpdateBusinessHourCmd::getId, Function.identity()));

        for (BusinessHour businessHour : store.getBusinessHours()) {
            UpdateBusinessHourCmd updateBusinessHourCmd = updateBusinessHourCmdMap.get(businessHour.getId());

            if (null == updateBusinessHourCmd) {
                throw new BusinessException(String.format(ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG,
                        businessHour.getDayOfWeek().toString()));
            }

            businessHour.updateBusinessHour(
                    updateBusinessHourCmd.getDayOfWeek(),
                    updateBusinessHourCmd.getOpeningTime(),
                    updateBusinessHourCmd.getClosingTime(),
                    updateBusinessHourCmd.isClosed());
        }

        Store updatedStore = storeRepository.update(store);

        return storeDataMapper.toStoreDetailDto(updatedStore);
    }
}
