package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.ProductImageId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ProductImage {

    private ProductImageId id;
    private ProductId productId;

    private String imageUrl;
    private Integer displayOrder;

    private Instant createdAt;
}
