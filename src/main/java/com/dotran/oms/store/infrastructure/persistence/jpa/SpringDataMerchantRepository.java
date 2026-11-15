package com.dotran.oms.store.infrastructure.persistence.jpa;

import com.dotran.oms.store.infrastructure.persistence.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataMerchantRepository extends JpaRepository<MerchantEntity, UUID> {

    boolean existsByCode(String code);
}
