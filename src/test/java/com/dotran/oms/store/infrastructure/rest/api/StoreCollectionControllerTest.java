package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.usecase.collection.AddProductCollectionUseCase;
import com.dotran.oms.store.application.usecase.collection.CreateStoreCollectionUseCase;
import com.dotran.oms.store.application.usecase.collection.GetListStoreCollectionUseCase;
import com.dotran.oms.store.application.usecase.collection.GetStoreCollectionUseCase;
import com.dotran.oms.store.application.usecase.collection.RemoveProductFromCollectionUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.UpsertStoreCollectionRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreCollectionResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.StoreCollectionRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreCollectionController")
class StoreCollectionControllerTest {

    @Mock private CreateStoreCollectionUseCase createStoreCollectionUseCase;
    @Mock private GetStoreCollectionUseCase getStoreCollectionUseCase;
    @Mock private GetListStoreCollectionUseCase getListStoreCollectionUseCase;
    @Mock private AddProductCollectionUseCase addProductCollectionUseCase;
    @Mock private RemoveProductFromCollectionUseCase removeProductCollectionUseCase;
    @Mock private StoreCollectionRestMapper restMapper;

    @InjectMocks
    private StoreCollectionController controller;

    @Test
    @DisplayName("create: maps request, enriches cmd, calls use case, maps response")
    void create_delegatesAndReturnsResponse() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UpsertStoreCollectionRequest request = mock(UpsertStoreCollectionRequest.class);
        CreateStoreCollectionCmd cmd = CreateStoreCollectionCmd.builder().name("test").build();
        StoreCollectionDto dto = mock(StoreCollectionDto.class);
        StoreCollectionResponse response = mock(StoreCollectionResponse.class);

        when(restMapper.fromRequestToCreateCmd(request)).thenReturn(cmd);
        when(createStoreCollectionUseCase.create(cmd)).thenReturn(dto);
        when(restMapper.fromDtoToResponse(dto)).thenReturn(response);

        StoreCollectionResponse result = controller.create(merchantId, storeId, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getAll: builds cmd, calls use case, maps each dto to response")
    void getAll_returnsListOfResponses() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        StoreCollectionDto dto = mock(StoreCollectionDto.class);
        StoreCollectionResponse response = mock(StoreCollectionResponse.class);

        when(getListStoreCollectionUseCase.getListCollectionByStoreId(any())).thenReturn(List.of(dto));
        when(restMapper.fromDtoToResponse(dto)).thenReturn(response);

        List<StoreCollectionResponse> result = controller.getAll(merchantId, storeId);

        assertThat(result).containsExactly(response);
    }

    @Test
    @DisplayName("getAll: returns empty list when no collections exist")
    void getAll_returnsEmptyList_whenNone() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(getListStoreCollectionUseCase.getListCollectionByStoreId(any())).thenReturn(List.of());

        List<StoreCollectionResponse> result = controller.getAll(merchantId, storeId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getDetails: builds cmd, calls use case, maps response")
    void getDetails_delegatesAndReturnsResponse() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        StoreCollectionDto dto = mock(StoreCollectionDto.class);
        StoreCollectionResponse response = mock(StoreCollectionResponse.class);

        when(getStoreCollectionUseCase.getCollectionById(any())).thenReturn(dto);
        when(restMapper.fromDtoToResponse(dto)).thenReturn(response);

        StoreCollectionResponse result = controller.getDetails(merchantId, storeId, collectionId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("addProducts: builds cmd, calls use case, maps response")
    void addProducts_delegatesAndReturnsResponse() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        StoreCollectionDto dto = mock(StoreCollectionDto.class);
        StoreCollectionResponse response = mock(StoreCollectionResponse.class);

        when(addProductCollectionUseCase.addProductsToCollection(any())).thenReturn(dto);
        when(restMapper.fromDtoToResponse(dto)).thenReturn(response);

        StoreCollectionResponse result = controller.addProducts(merchantId, storeId, collectionId, List.of(UUID.randomUUID()));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("removeProducts: builds cmd, calls use case, maps response")
    void removeProducts_delegatesAndReturnsResponse() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        StoreCollectionDto dto = mock(StoreCollectionDto.class);
        StoreCollectionResponse response = mock(StoreCollectionResponse.class);

        when(removeProductCollectionUseCase.removeProducts(any())).thenReturn(dto);
        when(restMapper.fromDtoToResponse(dto)).thenReturn(response);

        StoreCollectionResponse result = controller.removeProducts(merchantId, storeId, collectionId, List.of(UUID.randomUUID()));

        assertThat(result).isEqualTo(response);
    }
}
