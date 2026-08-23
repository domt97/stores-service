package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.example.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.example.store.application.command.collection.GetCollectionDetailCmd;
import com.dotran.example.store.application.command.collection.GetListCollectionCmd;
import com.dotran.example.store.application.command.collection.RemoveProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.usecase.collection.AddProductCollectionUseCase;
import com.dotran.example.store.application.usecase.collection.CreateStoreCollectionUseCase;
import com.dotran.example.store.application.usecase.collection.GetListStoreCollectionUseCase;
import com.dotran.example.store.application.usecase.collection.GetStoreCollectionUseCase;
import com.dotran.example.store.application.usecase.collection.RemoveProductFromCollectionUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.infrastructure.rest.dto.request.UpsertStoreCollectionRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreCollectionResponse;
import com.dotran.example.store.infrastructure.rest.mapper.StoreCollectionRestMapper;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Store Collection Management", description = "APIs for managing product collections within a store")
@WebAdapter
@RestController
@RequestMapping(value = "/v1")
@RequiredArgsConstructor
@Slf4j
public class StoreCollectionController {

    private final CreateStoreCollectionUseCase createStoreCollectionUseCase;
    private final GetStoreCollectionUseCase getStoreCollectionUseCase;
    private final GetListStoreCollectionUseCase getListStoreCollectionUseCase;
    private final AddProductCollectionUseCase addProductCollectionUseCase;
    private final RemoveProductFromCollectionUseCase removeProductCollectionUseCase;
    private final StoreCollectionRestMapper restMapper;

    @Operation(summary = "Create a collection", description = "Creates a new product collection in the specified store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Collection created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreCollectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @PostMapping("/tenants/{tenantId}/stores/{storeId}/collections")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreCollectionResponse create(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @RequestBody @Valid UpsertStoreCollectionRequest request) {
        CreateStoreCollectionCmd cmd = restMapper.fromRequestToCreateCmd(request);
        cmd.setTenantId(TenantId.of(tenantId));
        cmd.setStoreId(StoreId.of(storeId));

        StoreCollectionDto storeCollectionDto = createStoreCollectionUseCase.create(cmd);

        return restMapper.fromDtoToResponse(storeCollectionDto);
    }

    @Operation(summary = "Get all collections", description = "Retrieves all collection belong to the store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collections retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreCollectionResponse.class))),
    })
    @GetMapping("/tenants/{tenantId}/stores/{storeId}/collections")
    @ResponseStatus(HttpStatus.OK)
    public List<StoreCollectionResponse> getAll(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId) {
        GetListCollectionCmd cmd = GetListCollectionCmd.builder()
                .tenantId(TenantId.of(tenantId))
                .storeId(StoreId.of(storeId))
                .build();

        List<StoreCollectionDto> storeCollectionDtos = getListStoreCollectionUseCase.getListCollectionByStoreId(cmd);

        return storeCollectionDtos
                .stream()
                .map(restMapper::fromDtoToResponse)
                .toList();
    }

    @Operation(summary = "Get collection details", description = "Retrieves detailed information about a specific product collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreCollectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found", content = @Content)
    })
    @GetMapping("/tenants/{tenantId}/stores/{storeId}/collections/{collectionId}")
    @ResponseStatus(HttpStatus.OK)
    public StoreCollectionResponse getDetails(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Collection ID", required = true) @PathVariable UUID collectionId) {
        GetCollectionDetailCmd cmd = GetCollectionDetailCmd.builder()
                .tenantId(TenantId.of(tenantId))
                .storeId(StoreId.of(storeId))
                .storeCollectionId(StoreCollectionId.of(collectionId))
                .build();

        StoreCollectionDto storeCollectionDto = getStoreCollectionUseCase.getCollectionById(cmd);

        return restMapper.fromDtoToResponse(storeCollectionDto);
    }

    @Operation(summary = "Add products to collection", description = "Adds one or more products to an existing collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products added successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreCollectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection or products not found", content = @Content)
    })
    @PostMapping("/tenants/{tenantId}/stores/{storeId}/collections/{collectionId}/add-products")
    @ResponseStatus(HttpStatus.OK)
    public StoreCollectionResponse addProducts(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Collection ID", required = true) @PathVariable UUID collectionId,
            @Parameter(description = "List of Product IDs to add", required = true) @RequestParam List<UUID> productIds) {
        AddProductCollectionCmd cmd = AddProductCollectionCmd.builder()
                .tenantId(TenantId.of(tenantId))
                .storeId(StoreId.of(storeId))
                .storeCollectionId(StoreCollectionId.of(collectionId))
                .productIds(productIds.stream()
                        .map(ProductId::of).toList())
                .build();

        StoreCollectionDto storeCollectionDto = addProductCollectionUseCase.addProductsToCollection(cmd);

        return restMapper.fromDtoToResponse(storeCollectionDto);
    }

    @Operation(summary = "Remove products from collection", description = "Removes one or more products from an existing collection")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products removed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreCollectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Collection or products not found", content = @Content)
    })
    @PostMapping("/tenants/{tenantId}/stores/{storeId}/collections/{collectionId}/remove-products")
    @ResponseStatus(HttpStatus.OK)
    public StoreCollectionResponse removeProducts(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Parameter(description = "Collection ID", required = true) @PathVariable UUID collectionId,
            @Parameter(description = "List of Product IDs to remove", required = true) @RequestParam List<UUID> productIds) {
        RemoveProductCollectionCmd cmd = RemoveProductCollectionCmd.builder()
                .tenantId(TenantId.of(tenantId))
                .storeId(StoreId.of(storeId))
                .storeCollectionId(StoreCollectionId.of(collectionId))
                .productIds(productIds.stream()
                        .map(ProductId::of).toList())
                .build();

        StoreCollectionDto storeCollectionDto = removeProductCollectionUseCase.removeProducts(cmd);

        return restMapper.fromDtoToResponse(storeCollectionDto);
    }
}
