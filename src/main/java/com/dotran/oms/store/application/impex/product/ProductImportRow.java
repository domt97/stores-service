package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.domain.id.CategoryId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImportRow {

    private String reference;
    private String name;
    private String description;
    private String thumbnailUrl;
    private CategoryId categoryId;
}
