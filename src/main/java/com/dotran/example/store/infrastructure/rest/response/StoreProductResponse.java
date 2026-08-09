package com.dotran.example.store.infrastructure.rest.response;

import com.dotran.example.store.application.dto.StoreProductDetailDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StoreProductResponse extends StoreProductDetailDto {

    public StoreProductResponse(StoreProductDetailDto dto) {
        this.setId(dto.getId());
        this.setStoreId(dto.getStoreId());
        this.setName(dto.getName());
        this.setDescription(dto.getDescription());
        this.setCategoryId(dto.getCategoryId());
        this.setStatus(dto.getStatus());
        this.setSkus(dto.getSkus());
        this.setImages(dto.getImages());
        this.setCreatedAt(dto.getCreatedAt());
        this.setUpdatedAt(dto.getUpdatedAt());
    }
}
