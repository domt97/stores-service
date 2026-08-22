package com.dotran.example.store.application.service.storeconfig;

import com.dotran.example.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.mapper.StoreDataMapper;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.usecase.SettingStoreBusinessHourUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.exception.BusinessException;
import com.dotran.example.store.domain.exception.StoreNotFoundException;
import com.dotran.example.store.domain.model.BusinessHour;
import com.dotran.example.store.domain.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotran.example.store.common.constants.Constants.ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG;

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
