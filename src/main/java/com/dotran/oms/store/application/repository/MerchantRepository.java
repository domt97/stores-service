package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.MerchantInfo;
import com.dotran.oms.core.domain.id.MerchantId;

import java.util.Optional;

public interface MerchantRepository {

    Optional<MerchantInfo> findByMerchantId(MerchantId merchantId);
}
