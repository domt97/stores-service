package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.domain.model.Store;

import java.util.Optional;

public interface StoreRepository {

    Store create(Store store);

    Store update(Store store);

    Optional<Store> findByMerchantIdAndStoreId(MerchantId merchantId, StoreId storeId);

    Store close(Store store);

    Store reopen(Store store);

    boolean existsByMerchantIdAndStoreId(MerchantId merchantId, StoreId storeId);
}
