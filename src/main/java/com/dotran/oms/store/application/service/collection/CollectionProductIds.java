package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.constants.Constants;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
class CollectionProductIds {

    /** Null list → empty; duplicates removed, order kept; a null id is rejected. */
    static List<ProductId> distinct(List<ProductId> productIds) {
        if (null == productIds) {
            return new ArrayList<>();
        }

        Set<ProductId> distinctIds = new LinkedHashSet<>();
        for (ProductId productId : productIds) {
            if (null == productId || null == productId.getValue()) {
                throw new ValidationException(Constants.ERROR_MSG_PRODUCT_ID_REQUIRED);
            }
            distinctIds.add(productId);
        }

        return new ArrayList<>(distinctIds);
    }
}
