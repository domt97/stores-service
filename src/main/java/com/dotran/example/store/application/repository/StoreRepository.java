package com.dotran.example.store.application.repository;

import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.common.domain.valueobject.StoreId;

import java.util.Optional;

public interface StoreRepository {

    Store create(Store store);

    Store update(Store store);

    Optional<Store> findByTenantIdAndStoreId(TenantId tenantId, StoreId storeId);

    Store close(Store store);

    Store reopen(Store store);
}
