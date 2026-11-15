package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.GetCollectionDetailCmd;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetStoreCollectionService")
class GetStoreCollectionServiceTest {

    @Mock private StoreCollectionRepository repository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreCollectionMapper mapper;

    @InjectMocks
    private GetStoreCollectionService getStoreCollectionService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final StoreCollectionId collectionId = StoreCollectionId.generateId();

    private final GetCollectionDetailCmd cmd = GetCollectionDetailCmd.builder()
            .merchantId(merchantId)
            .storeId(storeId)
            .storeCollectionId(collectionId)
            .build();

    @Test
    @DisplayName("throws NotFoundException when the store does not belong to the merchant")
    void throwsException_whenStoreNotOwnedByMerchant() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> getStoreCollectionService.getCollectionById(cmd))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Store not found");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("throws NotFoundException when the collection is not in this store")
    void throwsException_whenNotFound() {
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.getByStoreIdAndId(eq(storeId), eq(collectionId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getStoreCollectionService.getCollectionById(cmd))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Collection not found");
    }

    @Test
    @DisplayName("returns mapped DTO when collection exists in the store")
    void returnsDto_whenFound() {
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

        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
        when(repository.getByStoreIdAndId(storeId, collectionId)).thenReturn(Optional.of(collection));
        when(mapper.toCollectionDto(collection)).thenReturn(expectedDto);

        StoreCollectionDto result = getStoreCollectionService.getCollectionById(cmd);

        assertThat(result).isEqualTo(expectedDto);
    }
}
