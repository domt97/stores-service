package com.dotran.oms.store.domain.model;

import java.util.List;

/**
 * What {@link StoreProduct#replace} changed. The SKUs carry their final, normalised values, so persistence only copies
 * them. Added SKUs have no id yet (assigned when stored).
 */
public record ProductChangeSet(boolean productFieldsChanged, boolean imagesChanged, List<ProductSku> added,
                               List<ProductSku> updated, List<ProductSku> removed) {

    public static ProductChangeSet none() {
        return new ProductChangeSet(false, false, List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return !productFieldsChanged && !imagesChanged && added.isEmpty() && updated.isEmpty() && removed.isEmpty();
    }
}
