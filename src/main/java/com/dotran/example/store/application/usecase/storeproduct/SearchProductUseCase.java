package com.dotran.example.store.application.usecase.storeproduct;

import com.dotran.example.store.application.command.storeproduct.SearchProductCmd;
import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.common.dto.PagedResult;

public interface SearchProductUseCase {

    PagedResult<StoreProductReviewDto> search(SearchProductCmd searchCmd, DomainPageRequest pageRequest);
}
