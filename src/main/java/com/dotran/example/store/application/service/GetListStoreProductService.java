package com.dotran.example.store.application.service;

import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.application.mapper.StoreProductMapper;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.repository.TenantRepository;
import com.dotran.example.store.application.usecase.GetListStoreProductUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.domain.model.TenantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetListStoreProductService implements GetListStoreProductUseCase {

    private final StoreProductRepository storeProductRepository;
    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;
    private final StoreProductMapper storeProductMapper;

    @Override
    @Transactional
    public List<StoreProductReviewDto> getListProductByStoreId(UUID tenantId, UUID storeId, DomainPageRequest pageRequest) {
        TenantInfo tenantInfo = tenantRepository.findByTenantId(TenantId.of(tenantId))
                .orElseThrow(() -> new NotFoundException("Tenant not found"));

        Store store = storeRepository.findByTenantIdAndStoreId(tenantInfo.getId(), StoreId.of(storeId))
                .orElseThrow(() -> new NotFoundException("Store not found"));

        List<StoreProduct> storeProducts = storeProductRepository.getListByStoreId(store.getId(), pageRequest);

        return storeProducts.stream()
                .map(storeProductMapper::fromStoreProductToPreview)
                .toList();
    }
}
