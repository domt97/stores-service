package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.model.StoreAvailability;
import com.dotran.oms.store.infrastructure.mapper.StoreAvailabilityPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreAvailabilityRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class StoreAvailabilityPersistenceAdapter implements StoreAvailabilityRepository {

    private final SpringDataStoreAvailabilityRepository repository;
    private final StoreAvailabilityPersistenceMapper mapper;

    @Override
    public StoreAvailability save(StoreAvailability storeAvailability) {
        StoreAvailabilityEntity entity = mapper.fromDomainToEntity(storeAvailability);
        StoreAvailabilityEntity savedEntity = repository.save(entity);

        return mapper.fromEntityToDomain(savedEntity);
    }

    @Override
    public Optional<StoreAvailability> findByIdAndStoreId(StoreAvailabilityId storeAvailabilityId, StoreId storeId) {
        return repository.findByIdAndStoreId(storeAvailabilityId.getValue(), storeId.getValue()).
                map(mapper::fromEntityToDomain);
    }
}
