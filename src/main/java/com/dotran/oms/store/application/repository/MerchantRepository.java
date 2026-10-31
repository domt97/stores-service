package com.dotran.oms.store.application.repository;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.domain.model.Merchant;

import java.util.Optional;

public interface MerchantRepository {

    /**
     * Inserts a new merchant. Throws {@code MerchantCodeAlreadyExistsException} if the code is already taken
     * (also when a concurrent registration wins the unique constraint {@code uk_merchants_code}).
     */
    Merchant create(Merchant merchant);

    Optional<Merchant> findById(MerchantId merchantId);

    boolean existsById(MerchantId merchantId);

    /** {@code code} must already be normalized ({@link Merchant#normalizeCode(String)}). */
    boolean existsByCode(String code);
}
