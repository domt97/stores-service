package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.model.StoreAvailability;
import com.dotran.oms.store.infrastructure.mapper.StoreAvailabilityPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreAvailabilityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreAvailabilityPersistenceAdapter")
class StoreAvailabilityPersistenceAdapterTest {

    @Mock private SpringDataStoreAvailabilityRepository repository;
    @Mock private StoreAvailabilityPersistenceMapper mapper;

    @InjectMocks
    private StoreAvailabilityPersistenceAdapter adapter;

    private StoreAvailability buildAvailability() {
        return StoreAvailability.builder()
                .id(StoreAvailabilityId.generateId())
                .storeId(StoreId.newStoreId())
                .type(AvailabilityType.HOLIDAY)
                .startTime(Instant.now())
                .cancelled(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("save: maps to entity, persists, and maps back to domain")
    void save_persistsAndReturnsDomain() {
        StoreAvailability availability = buildAvailability();
        StoreAvailabilityEntity entity = mock(StoreAvailabilityEntity.class);
        StoreAvailabilityEntity savedEntity = mock(StoreAvailabilityEntity.class);

        when(mapper.fromDomainToEntity(availability)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(savedEntity);
        when(mapper.fromEntityToDomain(savedEntity)).thenReturn(availability);

        StoreAvailability result = adapter.save(availability);

        assertThat(result).isEqualTo(availability);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("findByIdAndStoreId: returns mapped domain when found")
    void find_returnsDomain_whenFound() {
        StoreAvailabilityId id = StoreAvailabilityId.generateId();
        StoreId storeId = StoreId.newStoreId();
        StoreAvailabilityEntity entity = mock(StoreAvailabilityEntity.class);
        StoreAvailability availability = buildAvailability();

        when(repository.findByIdAndStoreId(id.getValue(), storeId.getValue()))
                .thenReturn(Optional.of(entity));
        when(mapper.fromEntityToDomain(entity)).thenReturn(availability);

        Optional<StoreAvailability> result = adapter.findByIdAndStoreId(id, storeId);

        assertThat(result).contains(availability);
    }

    @Test
    @DisplayName("findByIdAndStoreId: returns empty when not found")
    void find_returnsEmpty_whenNotFound() {
        StoreAvailabilityId id = StoreAvailabilityId.generateId();
        StoreId storeId = StoreId.newStoreId();

        when(repository.findByIdAndStoreId(id.getValue(), storeId.getValue()))
                .thenReturn(Optional.empty());

        Optional<StoreAvailability> result = adapter.findByIdAndStoreId(id, storeId);

        assertThat(result).isEmpty();
    }
}
