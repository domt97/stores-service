package com.dotran.example.store.application.service.storeproduct;

import com.dotran.example.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.application.mapper.StoreProductMapper;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.repository.TenantRepository;
import com.dotran.example.store.application.usecase.storeproduct.SearchProductUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.constants.Constants;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.common.dto.PagedResult;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.domain.model.TenantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SearchProductService implements SearchProductUseCase {

    private final StoreProductRepository repository;
    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final StoreProductMapper mapper;

    @Override
    @Transactional
    public PagedResult<StoreProductReviewDto> search(SearchProductCmd searchCmd, DomainPageRequest pageRequest) {
        TenantInfo tenantInfo = tenantRepository.findByTenantId(searchCmd.getTenantId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_TENANT_NOT_FOUND));

        Store store = storeRepository.findByTenantIdAndStoreId(tenantInfo.getId(), searchCmd.getStoreId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND));

        PagedResult<StoreProduct> storeProductPagedResult = repository
                .searchProducts(store.getId(), searchCmd.getPriceRange(), pageRequest);

        return storeProductPagedResult.map(mapper::fromStoreProductToPreview);
    }
}
