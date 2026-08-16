package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.usecase.CreateStoreProductUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreProductRestMapper;
import com.dotran.example.store.infrastructure.rest.response.StoreProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@WebAdapter
@RestController
@RequestMapping(value = "/v1/store/product")
@RequiredArgsConstructor
@Slf4j
public class StoreProductController {

    private final CreateStoreProductUseCase createStoreProductUseCase;
    private final StoreProductRestMapper storeProductRestMapper;

    @PostMapping("/{tenantId}/{storeId}")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductResponse create(@PathVariable UUID tenantId, @PathVariable UUID storeId,
                                       @RequestBody @Valid CreateStoreProductRequest request) {
        CreateStoreProductCmd createStoreProductCmd = storeProductRestMapper.fromCreateRequestToCmd(request);
        createStoreProductCmd.setTenantId(tenantId);
        createStoreProductCmd.setStoreId(storeId);

        StoreProductDetailDto storeProductDetailDto = createStoreProductUseCase.createProduct(createStoreProductCmd);

        return storeProductRestMapper.toStoreProductResponse(storeProductDetailDto);
    }
}
