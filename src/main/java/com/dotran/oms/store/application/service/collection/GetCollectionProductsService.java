package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.GetCollectionProductsCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.collection.GetCollectionProductsUseCase;
import com.dotran.oms.store.common.constants.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCollectionProductsService implements GetCollectionProductsUseCase {

    private final StoreRepository storeRepository;
    private final StoreCollectionRepository storeCollectionRepository;
    private final StoreProductRepository storeProductRepository;
    private final StoreProductMapper mapper;

    @Override
    // The transaction must cover the mapping too: previews read the lazily loaded SKUs (price range, SKU count).
    @Transactional(readOnly = true)
    public PagedResult<StoreProductReviewDto> getProducts(GetCollectionProductsCmd cmd, DomainPageRequest pageRequest) {
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        if (!storeCollectionRepository.existsByStoreIdAndId(cmd.getStoreId(), cmd.getStoreCollectionId())) {
            throw new NotFoundException(Constants.ERROR_MSG_COLLECTION_NOT_FOUND);
        }

        return storeProductRepository
                .findInCollection(cmd.getStoreId(), cmd.getStoreCollectionId(), pageRequest)
                .map(mapper::fromStoreProductToPreview);
    }
}
