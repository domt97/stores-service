package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.GetListCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetListStoreCollectionService")
class GetListStoreCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private GetListStoreCollectionService getListStoreCollectionService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();

    private final GetListCollectionCmd cmd = GetListCollectionCmd.builder()
            .merchantId(merchantId)
            .storeId(storeId)
            .build();

    @Test
    @DisplayName("throws NotFoundException when the store does not belong to the merchant")
    void throwsException_whenStoreNotOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> getListStoreCollectionService.getListCollectionByStoreId(cmd))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store not found");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("returns mapped DTOs for collections in the store")
    void returnsMappedDtos_forCollectionsInStore() {
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

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.getListCollectionByStoreId(storeId)).thenReturn(List.of(collection));
        when(mapper.toCollectionDto(collection)).thenReturn(dto);

        List<StoreCollectionDto> result = getListStoreCollectionService.getListCollectionByStoreId(cmd);

        assertThat(result).containsExactly(dto);
    }

    @Test
    @DisplayName("returns empty list when store has no collections")
    void returnsEmptyList_whenNoCollections() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.getListCollectionByStoreId(storeId)).thenReturn(List.of());

        List<StoreCollectionDto> result = getListStoreCollectionService.getListCollectionByStoreId(cmd);

        assertThat(result).isEmpty();
    }
}
