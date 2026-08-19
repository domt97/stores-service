package com.dotran.example.store.infrastructure.mapper;

import com.dotran.example.store.common.mapper.IdMapper;
import com.dotran.example.store.domain.model.ProductImage;
import com.dotran.example.store.domain.model.ProductSku;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.infrastructure.persistence.entity.ProductImageEntity;
import com.dotran.example.store.infrastructure.persistence.entity.ProductSkuEntity;
import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = IdMapper.class
)
public abstract class StoreProductPersistenceMapper {

    @Autowired
    protected IdMapper idMapper;

    //
    // Domain -> Entity
    //

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeProduct.storeId.value")
    @Mapping(target = "categoryId", source = "storeProduct.categoryId.value")
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "images", ignore = true)
    public abstract StoreProductEntity fromStoreProduct(StoreProduct storeProduct);

    @Mapping(target = "id", source = "productSku.id.value")
    @Mapping(target = "sku", source = "productSku.sku.value")
    @Mapping(target = "name", source = "productSku.name")
    @Mapping(target = "status", source = "productSku.status")
    @Mapping(target = "createdAt", source = "productSku.createdAt")
    @Mapping(target = "updatedAt", source = "productSku.updatedAt")
    @Mapping(target = "product", source = "product")
    public abstract ProductSkuEntity fromProductSku(ProductSku productSku, StoreProductEntity product);

    public List<ProductSkuEntity> fromProductSkus(List<ProductSku> productSkus, StoreProductEntity product) {
        return productSkus.stream()
                .map(sku -> fromProductSku(sku, product))
                .collect(Collectors.toList());
    }

    @Mapping(target = "id", source = "productImage.id.value")
    @Mapping(target = "createdAt", source = "productImage.createdAt")
    @Mapping(target = "product", source = "product")
    public abstract ProductImageEntity fromProductImage(ProductImage productImage, StoreProductEntity product);

    public List<ProductImageEntity> fromProductImages(List<ProductImage> productImages, StoreProductEntity product) {
        return productImages.stream()
                .map(image -> fromProductImage(image, product))
                .collect(Collectors.toList());
    }

    //
    // Entity -> Domain
    //

    @Mapping(target = "id", expression = "java(idMapper.toProductId(entity.getId()))")
    @Mapping(target = "storeId", expression = "java(idMapper.toStoreId(entity.getStoreId()))")
    @Mapping(target = "categoryId", expression = "java(idMapper.toCategoryId(entity.getCategoryId()))")
    @Mapping(target = "skus", source = "skus", qualifiedByName = "fromProductSkuEntities")
    @Mapping(target = "images", source = "images", qualifiedByName = "fromProductImageEntities")
    public abstract StoreProduct fromEntity(StoreProductEntity entity);

    @Mapping(target = "id", expression = "java(idMapper.toProductSkuId(entity.getId()))")
    @Mapping(target = "sku", expression = "java(com.dotran.example.store.common.domain.valueobject.SKU.of(entity.getSku()))")
    @Mapping(target = "productId", expression = "java(idMapper.toProductId(entity.getProduct().getId()))")
    public abstract ProductSku fromProductSkuEntity(ProductSkuEntity entity);

    @Named("fromProductSkuEntities")
    public List<ProductSku> fromProductSkuEntities(List<ProductSkuEntity> entities) {
        return entities.stream()
                .map(this::fromProductSkuEntity)
                .collect(Collectors.toList());
    }

    @Mapping(target = "id", expression = "java(idMapper.toProductImageId(entity.getId()))")
    @Mapping(target = "productId", expression = "java(idMapper.toProductId(entity.getProduct().getId()))")
    public abstract ProductImage fromProductImageEntity(ProductImageEntity entity);

    @Named("fromProductImageEntities")
    public List<ProductImage> fromProductImageEntities(List<ProductImageEntity> entities) {
        return entities.stream()
                .map(this::fromProductImageEntity)
                .collect(Collectors.toList());
    }
}
