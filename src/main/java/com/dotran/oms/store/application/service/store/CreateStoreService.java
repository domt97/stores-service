package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.cloud.dynamodb.DynamoDbTenantInfoRepository;
import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.store.CreateStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.store.CreateStoreUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateStoreService implements CreateStoreUseCase {

    private final StoreRepository storeRepository;
    private final DynamoDbTenantInfoRepository tenantRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreDetailDto create(CreateStoreCmd cmd) {
        TenantInfo tenantInfo = tenantRepository.findByTenantId(cmd.getTenantId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_TENANT_NOT_FOUND));
        Store store = Store.initStore(tenantInfo.getId(),
                cmd.getName(),
                cmd.getCode(),
                cmd.getOwnerId(),
                cmd.getEmail(),
                cmd.getPhone());

        store.addAddress(storeDataMapper.fromAddressCmdToAddress(cmd.getAddress()));
        store.addConfig(storeDataMapper.fromStoreConfigCmdToStoreConfig(cmd.getConfig()));
        store.addBusinessHour(storeDataMapper.fromListBusinessHourCmdToListBusinessHour(cmd.getBusinessHours()));

        Store createdStore = storeRepository.create(store);

        return storeDataMapper.toStoreDetailDto(createdStore);
    }
}
