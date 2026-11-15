package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal order quote request. 0..100 SKU codes (0 = store data only, e.g. a delivery quote); the codes themselves are
 * not validated here: a null, blank or unknown code comes back {@code found = false}, never a 400 for the whole store.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQuoteRequest {

    /** Same bound as {@code PlaceCheckout} lines ({@code maxItems: 100}). */
    public static final int MAX_SKUS = 100;

    @NotNull
    @Size(max = MAX_SKUS)
    private List<String> skus;
}
