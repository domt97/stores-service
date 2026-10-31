package com.dotran.oms.store.application.usecase.collection;

import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.store.application.command.collection.GetCollectionProductsCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;

public interface GetCollectionProductsUseCase {

    /** Products in the collection, ordered by name then id. */
    PagedResult<StoreProductReviewDto> getProducts(GetCollectionProductsCmd cmd, DomainPageRequest pageRequest);
}
