package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.application.usecase.storeproduct.CreateStoreProductUseCase;
import com.dotran.example.store.application.usecase.storeproduct.GetListStoreProductUseCase;
import com.dotran.example.store.application.usecase.storeproduct.GetStoreProductDetailUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.common.dto.PagedResult;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductPreviewResponse;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductResponse;
import com.dotran.example.store.infrastructure.rest.mapper.StoreProductRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.UUID;

@Tag(name = "Store Product Management", description = "APIs for managing products within a store, including creation, retrieval and listing")
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

    @Operation(summary = "Create a new product", description = "Creates a new product in the specified store with SKUs and images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @PostMapping("/tenants/{tenantId}/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductResponse create(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @RequestBody @Valid CreateStoreProductRequest request) {
        CreateStoreProductCmd createStoreProductCmd = storeProductRestMapper.fromCreateRequestToCmd(request);
        createStoreProductCmd.setTenantId(tenantId);
        createStoreProductCmd.setStoreId(storeId);

        StoreProductDetailDto storeProductDetailDto = createStoreProductUseCase.createProduct(createStoreProductCmd);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }

    @Operation(summary = "Get product details", description = "Retrieves detailed information about a specific product including SKUs and images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/tenants/{tenantId}/stores/{storeId}/products/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public StoreProductResponse getProductDetails(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Product ID", required = true) @PathVariable UUID productId) {
        StoreProductDetailDto storeProductDetailDto = getStoreProductDetailUseCase
                .getProductById(tenantId, storeId, productId);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }

    @Operation(summary = "List store products", description = "Retrieves a paginated list of products for a specific store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResult.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @GetMapping("/tenants/{tenantId}/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.OK)
    public PagedResult<StoreProductPreviewResponse> getStoreProducts(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") Integer pageNumber) {
        PagedResult<StoreProductReviewDto> storeProductReviewDtos = getListStoreProductUseCase
                .getListProductByStoreId(tenantId, storeId, DomainPageRequest.builder()
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .build());

        return storeProductReviewDtos.map(storeProductRestMapper::toStoreProductPreviewResponse);
    }
}
