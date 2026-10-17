package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
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
@DisplayName("StoreProductController")
class StoreProductControllerTest {

    @Mock private CreateStoreProductUseCase createStoreProductUseCase;
    @Mock private GetStoreProductDetailUseCase getStoreProductDetailUseCase;
    @Mock private SearchProductUseCase searchProductUseCase;
    @Mock private ImportStoreProductUseCase importStoreProductUseCase;
    @Mock private StoreProductRestMapper storeProductRestMapper;

    @InjectMocks
    private StoreProductController controller;

    @Test
    @DisplayName("create: maps request, enriches cmd, calls use case, maps response")
    void create_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        CreateStoreProductRequest request = mock(CreateStoreProductRequest.class);
        CreateStoreProductCmd cmd = mock(CreateStoreProductCmd.class);
        StoreProductDetailDto dto = mock(StoreProductDetailDto.class);
        StoreProductResponse response = mock(StoreProductResponse.class);

        when(storeProductRestMapper.fromCreateRequestToCmd(request)).thenReturn(cmd);
        when(createStoreProductUseCase.createProduct(cmd)).thenReturn(dto);
        when(storeProductRestMapper.toStoreProductResponse(dto)).thenReturn(response);

        StoreProductResponse result = controller.create(tenantId, storeId, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getProductDetails: calls use case, maps response")
    void getProductDetails_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProductDetailDto dto = mock(StoreProductDetailDto.class);
        StoreProductResponse response = mock(StoreProductResponse.class);

        when(getStoreProductDetailUseCase.getProductById(tenantId, storeId, productId)).thenReturn(dto);
        when(storeProductRestMapper.toStoreProductResponse(dto)).thenReturn(response);

        StoreProductResponse result = controller.getProductDetails(tenantId, storeId, productId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getStoreProducts: calls use case, maps paged result")
    void getStoreProducts_returnsMappedPagedResult() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        StoreProductReviewDto reviewDto = mock(StoreProductReviewDto.class);
        StoreProductPreviewResponse previewResponse = mock(StoreProductPreviewResponse.class);

        PagedResult<StoreProductReviewDto> pagedDtos = PagedResult.<StoreProductReviewDto>builder()
                .content(List.of(reviewDto))
                .pageNumber(0).pageSize(10).totalElements(1).totalPages(1).isLast(true)
                .build();

        when(searchProductUseCase.search(any(), any())).thenReturn(pagedDtos);
        when(storeProductRestMapper.toStoreProductPreviewResponse(reviewDto)).thenReturn(previewResponse);

        PagedResult<StoreProductPreviewResponse> result = controller.getStoreProducts(tenantId, storeId, 10, 0, null, null);

        assertThat(result.getContent()).containsExactly(previewResponse);
    }

    @Test
    @DisplayName("importProducts: builds cmd, calls use case, returns result")
    void importProducts_delegatesAndReturnsImportResult() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        ImportStoreProductsRequest request = mock(ImportStoreProductsRequest.class);
        ImportResult importResult = mock(ImportResult.class);

        when(request.getPath()).thenReturn("bucket");
        when(request.getFileName()).thenReturn("file.xlsx");
        when(importStoreProductUseCase.execute(any())).thenReturn(importResult);

        ImportResult result = controller.importProducts(tenantId, storeId, request);

        assertThat(result).isEqualTo(importResult);
    }
}
