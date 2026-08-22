package com.dotran.example.store.application.service.storeproduct;

import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.mapper.StoreProductMapper;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.repository.TenantRepository;
import com.dotran.example.store.application.usecase.storeproduct.GetStoreProductDetailUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.constants.Constants;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.domain.model.TenantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetStoreProductDetailService implements GetStoreProductDetailUseCase {

    private final StoreProductRepository repository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final StoreProductMapper mapper;

    @Override
    @Transactional
    public StoreProductDetailDto getProductById(UUID tenantId, UUID storeId, UUID productId) {
        TenantInfo tenantInfo = tenantRepository.findByTenantId(TenantId.of(tenantId))
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_TENANT_NOT_FOUND));

        Store store = storeRepository.findByTenantIdAndStoreId(tenantInfo.getId(), StoreId.of(storeId))
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND));

        StoreProduct storeProduct = repository.getByStoreIdAndProductId(store.getId(), ProductId.of(productId))
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_PRODUCT_NOT_FOUND));

        return mapper.fromStoreProduct(storeProduct);
    }
}
