package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.StoreAvailabilityRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.common.domain.valueobject.StoreAvailabilityId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.domain.model.StoreAvailability;
import com.dotran.example.store.infrastructure.mapper.StoreAvailabilityPersistenceMapper;
import com.dotran.example.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
public class StoreAvailabilityPersistenceAdapter implements StoreAvailabilityRepository {

    private final SpringDataStoreAvailabilityRepository repository;
    private final StoreAvailabilityPersistenceMapper mapper;

    @Override
    @Transactional
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
