package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.store.infrastructure.rest.dto.request.UpdateStoreProductRequest;
import com.dotran.oms.store.application.command.storeproduct.UpdateStoreProductCmd;
import com.dotran.oms.store.application.usecase.storeproduct.UpdateStoreProductUseCase;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
import com.dotran.oms.store.application.usecase.impex.ImportStoreProductUseCase;
import com.dotran.oms.store.application.usecase.storeproduct.CreateStoreProductUseCase;
import com.dotran.oms.store.application.usecase.storeproduct.GetStoreProductDetailUseCase;
import com.dotran.oms.store.application.usecase.storeproduct.SearchProductUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import com.dotran.oms.store.infrastructure.rest.dto.request.ImportStoreProductsRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreProductPreviewResponse;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreProductResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.StoreProductRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreProductController")
class StoreProductControllerTest {

    @Mock private CreateStoreProductUseCase createStoreProductUseCase;
    @Mock private UpdateStoreProductUseCase updateStoreProductUseCase;
    @Mock private GetStoreProductDetailUseCase getStoreProductDetailUseCase;
    @Mock private SearchProductUseCase searchProductUseCase;
    @Mock private ImportStoreProductUseCase importStoreProductUseCase;
    @Mock private StoreProductRestMapper storeProductRestMapper;

    @InjectMocks
    private StoreProductController controller;

    @Test
    @DisplayName("create: maps request, enriches cmd, calls use case, maps response")
    void create_delegatesAndReturnsResponse() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        CreateStoreProductRequest request = mock(CreateStoreProductRequest.class);
        CreateStoreProductCmd cmd = mock(CreateStoreProductCmd.class);
        StoreProductDetailDto dto = mock(StoreProductDetailDto.class);
        StoreProductResponse response = mock(StoreProductResponse.class);

        when(storeProductRestMapper.fromCreateRequestToCmd(request)).thenReturn(cmd);
        when(createStoreProductUseCase.createProduct(cmd)).thenReturn(dto);
        when(storeProductRestMapper.toStoreProductResponse(dto)).thenReturn(response);

        StoreProductResponse result = controller.create(merchantId, storeId, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getProductDetails: calls use case, maps response")
    void getProductDetails_delegatesAndReturnsResponse() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProductDetailDto dto = mock(StoreProductDetailDto.class);
        StoreProductResponse response = mock(StoreProductResponse.class);

        when(getStoreProductDetailUseCase.getProductById(merchantId, storeId, productId)).thenReturn(dto);
        when(storeProductRestMapper.toStoreProductResponse(dto)).thenReturn(response);

        StoreProductResponse result = controller.getProductDetails(merchantId, storeId, productId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getStoreProducts: calls use case, maps paged result")
    void getStoreProducts_returnsMappedPagedResult() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        StoreProductReviewDto reviewDto = mock(StoreProductReviewDto.class);
        StoreProductPreviewResponse previewResponse = mock(StoreProductPreviewResponse.class);

        PagedResult<StoreProductReviewDto> pagedDtos = PagedResult.<StoreProductReviewDto>builder()
                .content(List.of(reviewDto))
                .pageNumber(0).pageSize(10).totalElements(1).totalPages(1).isLast(true)
                .build();

        ArgumentCaptor<SearchProductCmd> cmdCaptor = ArgumentCaptor.forClass(SearchProductCmd.class);
        ArgumentCaptor<DomainPageRequest> pageCaptor = ArgumentCaptor.forClass(DomainPageRequest.class);
        when(searchProductUseCase.search(cmdCaptor.capture(), pageCaptor.capture())).thenReturn(pagedDtos);
        when(storeProductRestMapper.toStoreProductPreviewResponse(reviewDto)).thenReturn(previewResponse);

        PagedResult<StoreProductPreviewResponse> result = controller.getStoreProducts(merchantId, storeId, 10, 0, null, null, null);

        assertThat(result.getContent()).containsExactly(previewResponse);
        assertThat(cmdCaptor.getValue().getNotInCollectionId()).isNull();
        assertThat(pageCaptor.getValue().getSortBy()).isNull();
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("getStoreProducts: notInCollectionId reaches the command")
    void getStoreProducts_passesNotInCollectionId() {
        UUID collectionId = UUID.randomUUID();
        PagedResult<StoreProductReviewDto> empty = PagedResult.<StoreProductReviewDto>builder()
                .content(List.of()).pageNumber(0).pageSize(20).totalElements(0).totalPages(0).isLast(true).build();
        ArgumentCaptor<SearchProductCmd> cmdCaptor = ArgumentCaptor.forClass(SearchProductCmd.class);
        when(searchProductUseCase.search(cmdCaptor.capture(), any())).thenReturn(empty);

        controller.getStoreProducts(UUID.randomUUID(), UUID.randomUUID(), 20, 0, "createdAt", "DESC", collectionId);

        assertThat(cmdCaptor.getValue().getNotInCollectionId()).isEqualTo(StoreCollectionId.of(collectionId));
    }

    @Test
    @DisplayName("getStoreProducts: negative page or page size below 1 → ValidationException, use case never called")
    void getStoreProducts_invalidPage_rejected() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        assertThatThrownBy(() -> controller.getStoreProducts(merchantId, storeId, 0, 0, null, null, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> controller.getStoreProducts(merchantId, storeId, 10, -1, null, null, null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(searchProductUseCase);
    }

    @Test
    @DisplayName("importProducts: builds cmd, calls use case, returns result")
    void importProducts_delegatesAndReturnsImportResult() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        ImportStoreProductsRequest request = mock(ImportStoreProductsRequest.class);
        ImportResult importResult = mock(ImportResult.class);

        when(request.getPath()).thenReturn("bucket");
        when(request.getFileName()).thenReturn("file.xlsx");
        when(importStoreProductUseCase.execute(any())).thenReturn(importResult);

        ImportResult result = controller.importProducts(merchantId, storeId, request);

        assertThat(result).isEqualTo(importResult);
    }

    @Test
    @DisplayName("update puts the path merchant, store and product into the command and returns the mapped response")
    void update_usesPathIds() {
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UpdateStoreProductRequest request = UpdateStoreProductRequest.builder().version(3L).name("P").build();
        UpdateStoreProductCmd cmd = UpdateStoreProductCmd.builder().version(3L).name("P").build();
        StoreProductDetailDto dto = StoreProductDetailDto.builder().version(4).build();
        StoreProductResponse response = StoreProductResponse.builder().version(4).build();
        when(storeProductRestMapper.fromUpdateRequestToCmd(request)).thenReturn(cmd);
        when(updateStoreProductUseCase.updateProduct(cmd)).thenReturn(dto);
        when(storeProductRestMapper.toStoreProductResponse(dto)).thenReturn(response);

        StoreProductResponse result = controller.update(merchantId, storeId, productId, request);

        assertThat(result.getVersion()).isEqualTo(4);
        assertThat(cmd.getMerchantId()).isEqualTo(MerchantId.of(merchantId));
        assertThat(cmd.getStoreId()).isEqualTo(StoreId.of(storeId));
        assertThat(cmd.getProductId()).isEqualTo(ProductId.of(productId));
    }
}
