package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.store.common.id.ProductImageId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    private ProductImageId id;
    private ProductId productId;
    private String imageUrl;
    private Integer displayOrder;
    private Instant createdAt;

    public void init() {
        this.createdAt = Instant.now();
    }
}
