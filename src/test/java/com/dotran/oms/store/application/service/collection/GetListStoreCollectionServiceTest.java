package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.application.command.collection.GetListCollectionCmd;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetListStoreCollectionService")
class GetListStoreCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private GetListStoreCollectionService getListStoreCollectionService;

    @Test
    @DisplayName("returns mapped DTOs for collections in the store")
    void returnsMappedDtos_forCollectionsInStore() {
        StoreId storeId = StoreId.newStoreId();
        GetListCollectionCmd cmd = GetListCollectionCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(storeId)
                .build();

        StoreCollection collection = StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(storeId)
                .name("Summer Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>())
                .build();

        StoreCollectionDto dto = StoreCollectionDto.builder()
                .id(UUID.randomUUID())
                .name("Summer Collection")
                .build();

        when(repository.getListCollectionByStoreId(storeId)).thenReturn(List.of(collection));
        when(mapper.toCollectionDto(collection)).thenReturn(dto);

        List<StoreCollectionDto> result = getListStoreCollectionService.getListCollectionByStoreId(cmd);

        assertThat(result).containsExactly(dto);
    }

    @Test
    @DisplayName("returns empty list when store has no collections")
    void returnsEmptyList_whenNoCollections() {
        StoreId storeId = StoreId.newStoreId();
        GetListCollectionCmd cmd = GetListCollectionCmd.builder()
                .tenantId(TenantId.newTenantId())
                .storeId(storeId)
                .build();

        when(repository.getListCollectionByStoreId(storeId)).thenReturn(List.of());

        List<StoreCollectionDto> result = getListStoreCollectionService.getListCollectionByStoreId(cmd);

        assertThat(result).isEmpty();
    }
}
