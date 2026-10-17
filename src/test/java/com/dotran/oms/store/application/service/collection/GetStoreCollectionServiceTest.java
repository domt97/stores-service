package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.GetCollectionDetailCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
import com.dotran.oms.store.domain.model.StoreCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetStoreCollectionService")
class GetStoreCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private GetStoreCollectionService getStoreCollectionService;

    @Test
    @DisplayName("throws NotFoundException when collection does not exist")
    void throwsException_whenNotFound() {
        StoreCollectionId collectionId = StoreCollectionId.generateId();
        GetCollectionDetailCmd cmd = GetCollectionDetailCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(StoreId.newStoreId())
                .storeCollectionId(collectionId)
                .build();

        when(repository.getById(collectionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreCollectionService.getCollectionById(cmd))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("returns mapped DTO when collection exists")
    void returnsDto_whenFound() {
        StoreId storeId = StoreId.newStoreId();
        StoreCollectionId collectionId = StoreCollectionId.generateId();
        GetCollectionDetailCmd cmd = GetCollectionDetailCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(storeId)
                .storeCollectionId(collectionId)
                .build();

        StoreCollection collection = StoreCollection.builder()
                .id(collectionId)
                .storeId(storeId)
                .name("Winter Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>())
                .build();

        StoreCollectionDto expectedDto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .name("Winter Collection")
                .build();

        when(repository.getById(collectionId)).thenReturn(Optional.of(collection));
        when(mapper.toCollectionDto(collection)).thenReturn(expectedDto);

        StoreCollectionDto result = getStoreCollectionService.getCollectionById(cmd);

        assertThat(result).isEqualTo(expectedDto);
    }
}
