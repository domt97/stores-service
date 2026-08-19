package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.application.usecase.CreateStoreProductUseCase;
import com.dotran.example.store.application.usecase.GetListStoreProductUseCase;
import com.dotran.example.store.application.usecase.GetStoreProductDetailUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreProductRestMapper;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductPreviewResponse;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@WebAdapter
@RestController
@RequestMapping(value = "/v1")
@RequiredArgsConstructor
@Slf4j
public class StoreProductController {

    private final CreateStoreProductUseCase createStoreProductUseCase;
    private final GetStoreProductDetailUseCase getStoreProductDetailUseCase;
    private final GetListStoreProductUseCase getListStoreProductUseCase;
    private final StoreProductRestMapper storeProductRestMapper;

    @PostMapping("/tenants/{tenantId}/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductResponse create(@PathVariable UUID tenantId, @PathVariable UUID storeId,
                                       @RequestBody @Valid CreateStoreProductRequest request) {
        CreateStoreProductCmd createStoreProductCmd = storeProductRestMapper.fromCreateRequestToCmd(request);
        createStoreProductCmd.setTenantId(tenantId);
        createStoreProductCmd.setStoreId(storeId);

        StoreProductDetailDto storeProductDetailDto = createStoreProductUseCase.createProduct(createStoreProductCmd);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }

    @GetMapping("/tenants/{tenantId}/stores/{storeId}/products/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public StoreProductResponse getProductDetails(@PathVariable UUID tenantId,
                                                  @PathVariable UUID storeId,
                                                  @PathVariable UUID productId) {
        StoreProductDetailDto storeProductDetailDto = getStoreProductDetailUseCase
                .getProductById(tenantId, storeId, productId);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }

    @GetMapping("/tenants/{tenantId}/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.OK)
    public List<StoreProductPreviewResponse> getStoreProducts(@PathVariable UUID tenantId,
                                                              @PathVariable UUID storeId,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(defaultValue = "0") Integer pageNumber) {
        List<StoreProductReviewDto> storeProductReviewDtos = getListStoreProductUseCase
                .getListProductByStoreId(tenantId, storeId, DomainPageRequest.builder()
                        .page(pageNumber)
                        .size(pageSize)
                        .build());

        return storeProductRestMapper.toStoreProductPreviewResponseList(storeProductReviewDtos);
    }
}
