package com.dotran.example.store.application.repository;

import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.model.TenantInfo;

import java.util.Optional;

public interface TenantRepository {

    /**
     * Find tenant info by tenant ID.
     *
     * @param tenantId the tenant ID
     * @return optional containing the tenant info if found
     */
    Optional<TenantInfo> findByTenantId(TenantId tenantId);

    /**
     * Save a new tenant info.
     *
     * @param tenantInfo the tenant info to save
     */
    void save(TenantInfo tenantInfo);

    /**
     * Update existing tenant info.
     *
     * @param tenantInfo the tenant info to update
     */
    void update(TenantInfo tenantInfo);

    /**
     * Delete tenant info by tenant ID.
     *
     * @param tenantId the tenant ID
     */
    void delete(TenantId tenantId);
}

