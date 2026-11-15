package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.infrastructure.mapper.StorePersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorePersistenceAdapter")
class StorePersistenceAdapterTest {

    @Mock private SpringDataStoreRepository springDataStoreRepository;
    @Mock private StorePersistenceMapper storePersistenceMapper;

    @InjectMocks
    private StorePersistenceAdapter storePersistenceAdapter;

    private Store activeStore() {
        return Store.builder()
                .id(StoreId.newStoreId())
                .merchantId(MerchantId.newMerchantId())
                .name("Test Store")
                .code("TST001")
                .status(StoreStatus.ACTIVE)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("create: maps domain to entity and saves via JPA")
    void create_savesEntityAndReturnsDomain() {
        Store store = activeStore();
        StoreEntity entity = mock(StoreEntity.class);
        StoreEntity savedEntity = mock(StoreEntity.class);

        when(storePersistenceMapper.fromStoreToEntity(store)).thenReturn(entity);
        when(springDataStoreRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(storePersistenceMapper.fromEntityToStore(savedEntity)).thenReturn(store);

        Store result = storePersistenceAdapter.create(store);

        assertThat(result).isEqualTo(store);
        verify(springDataStoreRepository).saveAndFlush(entity);
    }

    @Test
    @DisplayName("findByMerchantIdAndStoreId: returns mapped store when found")
    void find_returnsMappedStore_whenFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();
        StoreEntity entity = mock(StoreEntity.class);
        Store expectedStore = activeStore();

        when(springDataStoreRepository.findByMerchantIdAndId(merchantId.getValue(), storeId.getValue()))
                .thenReturn(Optional.of(entity));
        when(storePersistenceMapper.fromEntityToStore(entity)).thenReturn(expectedStore);

        Optional<Store> result = storePersistenceAdapter.findByMerchantIdAndStoreId(merchantId, storeId);

        assertThat(result).contains(expectedStore);
    }

    @Test
    @DisplayName("findByMerchantIdAndStoreId: returns empty when not found")
    void find_returnsEmpty_whenNotFound() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();

        when(springDataStoreRepository.findByMerchantIdAndId(merchantId.getValue(), storeId.getValue()))
                .thenReturn(Optional.empty());

        Optional<Store> result = storePersistenceAdapter.findByMerchantIdAndStoreId(merchantId, storeId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByMerchantIdAndStoreId: delegates to spring data repository")
    void exists_delegatesToJpaRepo() {
        MerchantId merchantId = MerchantId.newMerchantId();
        StoreId storeId = StoreId.newStoreId();

        when(springDataStoreRepository.existsByMerchantIdAndId(merchantId.getValue(), storeId.getValue()))
                .thenReturn(true);

        assertThat(storePersistenceAdapter.existsByMerchantIdAndStoreId(merchantId, storeId)).isTrue();
    }

    @Test
    @DisplayName("update: finds entity, updates it, and returns mapped domain")
    void update_updatesEntityAndReturnsDomain() {
        Store store = activeStore();
        StoreEntity entity = mock(StoreEntity.class);
        StoreEntity savedEntity = mock(StoreEntity.class);

        when(springDataStoreRepository.findByMerchantIdAndId(store.getMerchantId().getValue(), store.getId().getValue()))
                .thenReturn(Optional.of(entity));
        when(springDataStoreRepository.save(entity)).thenReturn(savedEntity);
        when(storePersistenceMapper.fromEntityToStore(savedEntity)).thenReturn(store);

        Store result = storePersistenceAdapter.update(store);

        assertThat(result).isEqualTo(store);
        verify(springDataStoreRepository).save(entity);
    }

    @Test
    @DisplayName("update: throws NotFoundException when store does not exist")
    void update_throwsNotFoundException_whenNotFound() {
        Store store = activeStore();

        when(springDataStoreRepository.findByMerchantIdAndId(store.getMerchantId().getValue(), store.getId().getValue()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storePersistenceAdapter.update(store))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("close: finds entity, updates it, and returns mapped domain")
    void close_updatesEntityAndReturnsDomain() {
        Store store = activeStore();
        StoreEntity entity = mock(StoreEntity.class);
        StoreEntity savedEntity = mock(StoreEntity.class);

        when(springDataStoreRepository.findByMerchantIdAndId(store.getMerchantId().getValue(), store.getId().getValue()))
                .thenReturn(Optional.of(entity));
        when(springDataStoreRepository.save(entity)).thenReturn(savedEntity);
        when(storePersistenceMapper.fromEntityToStore(savedEntity)).thenReturn(store);

        Store result = storePersistenceAdapter.close(store);

        assertThat(result).isEqualTo(store);
        verify(springDataStoreRepository).save(entity);
    }

    @Test
    @DisplayName("close: throws NotFoundException when store does not exist")
    void close_throwsNotFoundException_whenNotFound() {
        Store store = activeStore();

        when(springDataStoreRepository.findByMerchantIdAndId(store.getMerchantId().getValue(), store.getId().getValue()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storePersistenceAdapter.close(store))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("reopen: finds entity, updates it, and returns mapped domain")
    void reopen_updatesEntityAndReturnsDomain() {
        Store store = activeStore();
        StoreEntity entity = mock(StoreEntity.class);
        StoreEntity savedEntity = mock(StoreEntity.class);

        when(springDataStoreRepository.findByMerchantIdAndId(store.getMerchantId().getValue(), store.getId().getValue()))
                .thenReturn(Optional.of(entity));
        when(springDataStoreRepository.save(entity)).thenReturn(savedEntity);
        when(storePersistenceMapper.fromEntityToStore(savedEntity)).thenReturn(store);

        Store result = storePersistenceAdapter.reopen(store);

        assertThat(result).isEqualTo(store);
        verify(springDataStoreRepository).save(entity);
    }

    @Test
    @DisplayName("reopen: throws NotFoundException when store does not exist")
    void reopen_throwsNotFoundException_whenNotFound() {
        Store store = activeStore();

        when(springDataStoreRepository.findByMerchantIdAndId(store.getMerchantId().getValue(), store.getId().getValue()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storePersistenceAdapter.reopen(store))
                .isInstanceOf(NotFoundException.class);
    }
}
