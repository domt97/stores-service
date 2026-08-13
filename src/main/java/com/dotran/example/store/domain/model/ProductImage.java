package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.ProductImageId;
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
