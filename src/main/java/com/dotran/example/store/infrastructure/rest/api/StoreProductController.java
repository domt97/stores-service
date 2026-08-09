package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.mapper.StoreProductMapper;
import com.dotran.example.store.application.usecase.CreateStoreProductUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
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
    private final StoreProductMapper mapper;

    @PostMapping("{tenantId}/store/{storeId}/product")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductResponse create(@PathVariable String tenantId, @PathVariable String storeId,
                                       @RequestBody @Valid CreateStoreProductRequest request) {
        CreateStoreProductCmd createStoreProductCmd = mapper.fromCreateRequestToCmd(request);
        // set path variables
        createStoreProductCmd.setTenantId(UUID.fromString(tenantId));
        createStoreProductCmd.setStoreId(UUID.fromString(storeId));

        StoreProductDetailDto storeProductDetailDto = createStoreProductUseCase.createProduct(createStoreProductCmd);

        return new StoreProductResponse(storeProductDetailDto);
    }
}
