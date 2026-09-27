package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.domain.model.Store;

import java.util.Optional;

public interface StoreRepository {

    Store create(Store store);

    Store update(Store store);

    Optional<Store> findByTenantIdAndStoreId(TenantId tenantId, StoreId storeId);

    Store close(Store store);

    Store reopen(Store store);

    boolean existsByTenantIdAndStoreId(TenantId tenantId, StoreId storeId);
}
