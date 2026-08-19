package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.AddProductCollectionCmd;
import com.dotran.example.store.application.command.CreateStoreCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.usecase.AddProductCollectionUseCase;
import com.dotran.example.store.application.usecase.CreateStoreCollectionUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreCollectionId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.infrastructure.rest.dto.request.UpsertStoreCollectionRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreCollectionResponse;
import com.dotran.example.store.infrastructure.rest.mapper.StoreCollectionRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
public class StoreCollectionController {

    private final CreateStoreCollectionUseCase createStoreCollectionUseCase;
    private final AddProductCollectionUseCase addProductCollectionUseCase;
    private final StoreCollectionRestMapper restMapper;

    @PostMapping("/tenants/{tenantId}/stores/{storeId}/collections")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreCollectionResponse create(@PathVariable UUID tenantId,
                                          @PathVariable UUID storeId,
                                          @RequestBody @Valid UpsertStoreCollectionRequest request) {
        CreateStoreCollectionCmd cmd = restMapper.fromRequestToCreateCmd(request);
        cmd.setTenantId(TenantId.of(tenantId));
        cmd.setStoreId(StoreId.of(storeId));

        StoreCollectionDto storeCollectionDto = createStoreCollectionUseCase.create(cmd);

        return restMapper.fromDtoToResponse(storeCollectionDto);
    }

    @PostMapping("/tenants/{tenantId}/stores/{storeId}/collections/{collectionId}/add-products")
    @ResponseStatus(HttpStatus.OK)
    public StoreCollectionResponse addProducts(@PathVariable UUID tenantId,
                                               @PathVariable UUID storeId,
                                               @PathVariable UUID collectionId,
                                               @RequestParam List<UUID> productIds) {
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
}
