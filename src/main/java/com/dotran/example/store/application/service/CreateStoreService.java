package com.dotran.example.store.application.service;

import com.dotran.example.store.application.command.CreateStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.mapper.StoreDataMapper;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.repository.TenantRepository;
import com.dotran.example.store.application.usecase.CreateStoreUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.CustomerId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.domain.model.TenantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateStoreService implements CreateStoreUseCase {

    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreDetailDto create(CreateStoreCmd cmd) {
        TenantInfo tenantInfo = tenantRepository.findByTenantId(TenantId.of(cmd.getTenantId()))
                .orElseThrow(() -> new NotFoundException("Tenant not found with ID: " + cmd.getTenantId()));
        Store store = Store.initStore(tenantInfo.getId(),
                cmd.getName(),
                cmd.getCode(),
                CustomerId.of(cmd.getOwnerId()),
                cmd.getEmail(),
                cmd.getPhone());

        store.addAddress(storeDataMapper.fromAddressCmdToAddress(cmd.getAddress()));
        store.addConfig(storeDataMapper.fromStoreConfigCmdToStoreConfig(cmd.getConfig()));
        store.addBusinessHour(storeDataMapper.fromListBusinessHourCmdToListBusinessHour(cmd.getBusinessHours()));

        Store createdStore = storeRepository.create(store);

        return storeDataMapper.toStoreDetailDto(createdStore);
    }
}
