package com.dotran.oms.store.infrastructure.rest.api;

import org.springframework.web.bind.annotation.PutMapping;
import com.dotran.oms.store.infrastructure.rest.dto.request.UpdateStoreProductRequest;
import com.dotran.oms.store.application.usecase.storeproduct.UpdateStoreProductUseCase;
import com.dotran.oms.store.application.command.storeproduct.UpdateStoreProductCmd;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.annotation.WebAdapter;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.impex.ImportStoreProductCmd;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.command.storeproduct.SearchProductCmd;
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

import com.dotran.oms.store.common.id.StoreCollectionId;

import java.util.UUID;

@Tag(name = "Store Product Management", description = "APIs for managing products within a store, including creation, retrieval and listing")
@WebAdapter
@RestController
@RequestMapping(value = "/v1/store")
@RequiredArgsConstructor
@Slf4j
public class StoreProductController {

    private final CreateStoreProductUseCase createStoreProductUseCase;
    private final UpdateStoreProductUseCase updateStoreProductUseCase;
    private final GetStoreProductDetailUseCase getStoreProductDetailUseCase;
    private final SearchProductUseCase searchProductUseCase;
    private final ImportStoreProductUseCase importStoreProductUseCase;
    private final StoreProductRestMapper storeProductRestMapper;

    @Operation(summary = "Create a new product", description = "Creates a new product in the specified store with SKUs and images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProductResponse.class))),
            @ApiResponse(responseCode = "409", description = "A SKU code is already used by another product of the store", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request body, a currency that is not a 3-letter code, a negative price, more than 2 decimals, or a VND price that is not whole", content = @Content),
            @ApiResponse(responseCode = "404", description = "Merchant not found, or store not found for this merchant", content = @Content)
    })
    @PostMapping("/merchants/{merchantId}/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductResponse create(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @RequestBody @Valid CreateStoreProductRequest request) {
        CreateStoreProductCmd createStoreProductCmd = storeProductRestMapper.fromCreateRequestToCmd(request);
        createStoreProductCmd.setMerchantId(MerchantId.of(merchantId));
        createStoreProductCmd.setStoreId(StoreId.of(storeId));

        StoreProductDetailDto storeProductDetailDto = createStoreProductUseCase.createProduct(createStoreProductCmd);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }

    @Operation(summary = "Update a product",
            description = "Full replace of a product: details, status, images and SKUs. Send the version from the last GET. "
                    + "SKUs are matched by code; codes left out are removed (their codes stay reserved in the store) and "
                    + "codes sent again are restored. Omitted optional fields are cleared. A body equal to the current state "
                    + "returns 200 without writing; every SKU in it must pass the SKU price rule, so a stored SKU that "
                    + "breaks the rule must be corrected (400). Publishes ProductUpdated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated (or already in the requested state)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body: limits (100 SKUs, 20 images, 5000-character description), "
                    + "duplicate or blank SKU codes, a SKU id that does not match its code, or the SKU price rule", content = @Content),
            @ApiResponse(responseCode = "404", description = "Merchant, store or product not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "The product changed since the given version, or a SKU code is used by "
                    + "another product of the store", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status change not allowed (never back to DRAFT; ACTIVE needs an ACTIVE SKU)",
                    content = @Content)
    })
    @PutMapping("/merchants/{merchantId}/stores/{storeId}/products/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public StoreProductResponse update(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Product ID", required = true) @PathVariable UUID productId,
            @RequestBody @Valid UpdateStoreProductRequest request) {
        UpdateStoreProductCmd cmd = storeProductRestMapper.fromUpdateRequestToCmd(request);
        cmd.setMerchantId(MerchantId.of(merchantId));
        cmd.setStoreId(StoreId.of(storeId));
        cmd.setProductId(ProductId.of(productId));

        return storeProductRestMapper.toStoreProductResponse(updateStoreProductUseCase.updateProduct(cmd));
    }

    @Operation(summary = "Get product details", description = "Retrieves detailed information about a specific product including SKUs and images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/merchants/{merchantId}/stores/{storeId}/products/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public StoreProductResponse getProductDetails(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Product ID", required = true) @PathVariable UUID productId) {
        StoreProductDetailDto storeProductDetailDto = getStoreProductDetailUseCase
                .getProductById(merchantId, storeId, productId);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }

    @Operation(summary = "Search list store products", description = "Retrieves a paginated list of products for a specific store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid page number or size, sort field or direction", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found, or notInCollectionId is not a collection of this store", content = @Content)
    })
    @GetMapping("/merchants/{merchantId}/stores/{storeId}/products/search")
    @ResponseStatus(HttpStatus.OK)
    public PagedResult<StoreProductPreviewResponse> getStoreProducts(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") Integer pageNumber,
            @Parameter(description = "Sort field: createdAt (default, DESC), updatedAt, name or id; id is always the last sort key", example = "createdAt") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction", example = "DESC") @RequestParam(required = false) String direction,
            @Parameter(description = "Leave out the products already in this collection of the store") @RequestParam(required = false) UUID notInCollectionId) {
        // No upper bound here: the storefront relies on larger pages (oms-workspace integration.md, Front-end consumers).
        RestPaging.check(pageNumber, pageSize);
        SearchProductCmd searchProductCmd = SearchProductCmd.builder()
                .merchantId(MerchantId.of(merchantId))
                .storeId(StoreId.of(storeId))
                .notInCollectionId(notInCollectionId == null ? null : StoreCollectionId.of(notInCollectionId))
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PagedResult<StoreProductReviewDto> storeProductReviewDtos = searchProductUseCase
                .search(searchProductCmd, pageRequest);

        return storeProductReviewDtos.map(storeProductRestMapper::toStoreProductPreviewResponse);
    }

    @Operation(summary = "Import store products", description = "Imports a list of products for a specific store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products imported successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImportResult.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @PostMapping("/merchants/{merchantId}/stores/{storeId}/products/import")
    @ResponseStatus(HttpStatus.OK)
    public ImportResult importProducts(@PathVariable UUID merchantId,
                                       @PathVariable UUID storeId,
                                       @RequestBody ImportStoreProductsRequest request) {

        ImportStoreProductCmd importStoreProductCmd = ImportStoreProductCmd.builder()
                .merchantId(MerchantId.of(merchantId))
                .storeId(StoreId.of(storeId))
                .path(request.getPath())
                .fileName(request.getFileName())
                .build();

        return importStoreProductUseCase.execute(importStoreProductCmd);
    }
}
