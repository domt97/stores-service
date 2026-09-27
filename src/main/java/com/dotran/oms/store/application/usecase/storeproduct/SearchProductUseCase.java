package com.dotran.oms.store.application.usecase.storeproduct;

import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;

public interface SearchProductUseCase {

    PagedResult<StoreProductReviewDto> search(SearchProductCmd searchCmd, DomainPageRequest pageRequest);
}
