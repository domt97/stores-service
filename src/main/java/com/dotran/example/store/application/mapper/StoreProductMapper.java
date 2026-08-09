package com.dotran.example.store.application.mapper;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.ProductImageDto;
import com.dotran.example.store.application.dto.ProductSkuDto;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.common.domain.valueobject.CategoryId;
import com.dotran.example.store.domain.model.ProductImage;
import com.dotran.example.store.domain.model.ProductSku;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {com.dotran.example.store.common.mapper.IdMapper.class})
public abstract class StoreProductMapper {

    public abstract CreateStoreProductCmd fromCreateRequestToCmd(CreateStoreProductRequest request);

    @Mapping(target = "storeId", source = "storeId")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "skus", source = "skus")
    @Mapping(target = "images", source = "images")
    public abstract StoreProduct fromCreateStoreProductCmd(CreateStoreProductCmd cmd);

    // mapping helpers for nested command -> domain
    public abstract ProductSku fromProductSkuCmd(com.dotran.example.store.application.command.ProductSkuCmd cmd);
    public abstract ProductImage fromProductImageCmd(com.dotran.example.store.application.command.ProductImageCmd cmd);


    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    @Mapping(target = "categoryId", source = "categoryId.value")
    @Mapping(target = "skus", source = "skus", qualifiedByName = "fromProductSkus")
    @Mapping(target = "images", source = "images", qualifiedByName = "fromProductImages")
    public abstract StoreProductDetailDto fromStoreProduct(StoreProduct storeProduct);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "sku", source = "sku.value")
    @Mapping(target = "productId", source = "productId.value")
    public abstract ProductSkuDto fromProductSku(ProductSku productSku);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "productId", source = "productId.value")
    public abstract ProductImageDto fromProductImage(ProductImage productImage);

    @Named("fromProductSkus")
    public List<ProductSkuDto> fromProductSkus(List<ProductSku> productSkus) {
        return productSkus.stream()
                .map(this::fromProductSku)
                .toList();
    }

    @Named("fromProductImages")
    public List<ProductImageDto> fromProductImages(List<ProductImage> productImages) {
        return productImages.stream()
                .map(this::fromProductImage)
                .toList();
    }

    // helper mappings used by MapStruct to convert simple types
    protected com.dotran.example.store.common.domain.valueobject.StoreId map(java.util.UUID id) {
        return id == null ? null : com.dotran.example.store.common.domain.valueobject.StoreId.of(id);
    }

    protected CategoryId mapCatalog(java.util.UUID id) {
        return id == null ? null : CategoryId.of(id);
    }

    protected com.dotran.example.store.common.domain.valueobject.SKU mapSku(java.lang.String sku) {
        return sku == null ? null : com.dotran.example.store.common.domain.valueobject.SKU.of(sku);
    }
}
