package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.TenantId;

import java.util.Optional;

public interface TenantRepository {

    Optional<TenantInfo> findByTenantId(TenantId tenantId);
}
