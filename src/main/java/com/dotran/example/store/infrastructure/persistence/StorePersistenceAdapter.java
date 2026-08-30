package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.common.constants.Constants;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.infrastructure.mapper.StorePersistenceMapper;
import com.dotran.example.store.infrastructure.persistence.entity.StoreEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StorePersistenceAdapter implements StoreRepository {

    private final SpringDataStoreRepository springDataStoreRepository;
    private final StorePersistenceMapper storePersistenceMapper;

    @Override
    public Store create(Store store) {
        StoreEntity storeEntity = storePersistenceMapper.fromStoreToEntity(store);
        storeEntity.setAddress(storePersistenceMapper.fromAddressToStoreAddressEntity(store.getAddress(), storeEntity));
        storeEntity.setConfig(storePersistenceMapper.fromStoreConfigToStoreConfigEntity(store.getConfig(), storeEntity));
        storeEntity.setBusinessHours(storePersistenceMapper.fromBusinessHourListToEntities(store.getBusinessHours(), storeEntity));

        StoreEntity savedStore = springDataStoreRepository.saveAndFlush(storeEntity);

        return storePersistenceMapper.fromEntityToStore(savedStore);
    }

    @Override
    public Store update(Store store) {
        StoreEntity storeEntity = springDataStoreRepository
                .findByTenantIdAndId(store.getTenantId().getValue(), store.getId().getValue())
                .orElseThrow(NotFoundException::new);

        this.updateStore(storeEntity, store);

        StoreEntity closedStore = springDataStoreRepository.save(storeEntity);

        return storePersistenceMapper.fromEntityToStore(closedStore);
    }

    @Override
    public Optional<Store> findByTenantIdAndStoreId(TenantId tenantId, StoreId storeId) {
        return springDataStoreRepository.findByTenantIdAndId(tenantId.getValue(), storeId.getValue())
                .map(storePersistenceMapper::fromEntityToStore);
    }

    @Override
    public Store close(Store store) {
        StoreEntity storeEntity = springDataStoreRepository
                .findByTenantIdAndId(store.getTenantId().getValue(), store.getId().getValue())
                .orElseThrow(NotFoundException::new);

        this.updateStore(storeEntity, store);

        StoreEntity closedStore = springDataStoreRepository.save(storeEntity);

        return storePersistenceMapper.fromEntityToStore(closedStore);
    }

    @Override
    public Store reopen(Store store) {
        StoreEntity storeEntity = springDataStoreRepository
                .findByTenantIdAndId(store.getTenantId().getValue(), store.getId().getValue())
                .orElseThrow(NotFoundException::new);

        this.updateStore(storeEntity, store);

        StoreEntity closedStore = springDataStoreRepository.save(storeEntity);

        return storePersistenceMapper.fromEntityToStore(closedStore);
    }

    @Override
    public boolean existsByTenantIdAndStoreId(TenantId tenantId, StoreId storeId) {
        return springDataStoreRepository.existsByTenantIdAndId(tenantId.getValue(), storeId.getValue());
    }

    private void updateStore(StoreEntity storeEntity, Store store) {
        storePersistenceMapper.updateStoreEntity(storeEntity, store);
        storePersistenceMapper.updateStoreAddress(storeEntity.getAddress(), store.getAddress());
        storePersistenceMapper.updateStoreConfig(storeEntity.getConfig(), store.getConfig());
        storePersistenceMapper.updateBusinessHours(storeEntity.getBusinessHours(), store.getBusinessHours());
    }
}
