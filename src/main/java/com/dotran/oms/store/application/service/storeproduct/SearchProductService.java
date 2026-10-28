package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.storeproduct.SearchProductUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SearchProductService implements SearchProductUseCase {

    private final StoreProductRepository repository;
    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreCollectionRepository storeCollectionRepository;
    private final StoreProductMapper mapper;

    @Override
    @Transactional
    public PagedResult<StoreProductReviewDto> search(SearchProductCmd searchCmd, DomainPageRequest pageRequest) {
        if (!merchantRepository.existsById(searchCmd.getMerchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }

        Store store = storeRepository.findByMerchantIdAndStoreId(searchCmd.getMerchantId(), searchCmd.getStoreId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND));

        if (searchCmd.getNotInCollectionId() != null
                && !storeCollectionRepository.existsByStoreIdAndId(store.getId(), searchCmd.getNotInCollectionId())) {
            throw new NotFoundException(Constants.ERROR_MSG_COLLECTION_NOT_FOUND);
        }

        PagedResult<StoreProduct> storeProductPagedResult = repository
                .searchProducts(store.getId(), searchCmd.getPriceRange(), searchCmd.getNotInCollectionId(), pageRequest);

        return storeProductPagedResult.map(mapper::fromStoreProductToPreview);
    }
}
