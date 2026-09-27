package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.cloud.dynamodb.DynamoDbTenantInfoRepository;
import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.storeproduct.GetStoreProductDetailUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetStoreProductDetailService implements GetStoreProductDetailUseCase {

    private final StoreProductRepository repository;
    private final DynamoDbTenantInfoRepository tenantRepository;
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
