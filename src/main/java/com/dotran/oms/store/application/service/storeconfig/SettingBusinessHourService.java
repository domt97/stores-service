package com.dotran.oms.store.application.service.storeconfig;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
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

import java.time.DayOfWeek;
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
    public StoreDetailDto setupBusinessHour(UUID merchantIdString, UUID storeIdString, List<UpdateBusinessHourCmd> updateBusinessHourCmds) {
        MerchantId merchantId = MerchantId.of(merchantIdString);
        StoreId storeId = StoreId.of(storeIdString);

        Store store = storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)
                .orElseThrow(StoreNotFoundException::new);

        Map<DayOfWeek, UpdateBusinessHourCmd> cmdMap = updateBusinessHourCmds.stream()
                .collect(Collectors.toMap(UpdateBusinessHourCmd::getDayOfWeek, Function.identity()));

        for (BusinessHour businessHour : store.getBusinessHours()) {
            if (!cmdMap.containsKey(businessHour.getDayOfWeek())) {
                throw new BusinessException(String.format(ERROR_MSG_STORE_MISSING_BUSINESS_HOUR_CONFIG,
                        businessHour.getDayOfWeek().toString()));
            }
        }

        cmdMap.forEach((day, cmd) ->
                store.updateBusinessHour(day, cmd.getOpeningTime(), cmd.getClosingTime(), cmd.isClosed()));

        Store updatedStore = storeRepository.update(store);

        return storeDataMapper.toStoreDetailDto(updatedStore);
    }
}
