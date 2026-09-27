package com.dotran.oms.store.application.mapper;

import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.command.storeproduct.ProductImageCmd;
import com.dotran.oms.store.application.command.storeproduct.ProductSkuCmd;
import com.dotran.oms.store.application.dto.ProductImageDto;
import com.dotran.oms.store.application.dto.ProductSkuDto;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.dto.StoreProductReviewDto;
import com.dotran.oms.store.common.mapper.InternalIdMapper;
import com.dotran.oms.store.domain.model.ProductImage;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {InternalIdMapper.class})
public abstract class StoreProductMapper {

    @Mapping(target = "storeId", source = "storeId")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "skus", source = "skus", qualifiedByName = "fromProductSkuCmds")
    @Mapping(target = "images", source = "images", qualifiedByName = "fromProductImageCmds")
    public abstract StoreProduct fromCreateStoreProductCmd(CreateStoreProductCmd cmd);

    // mapping helpers for nested command -> domain
    public abstract ProductSku fromProductSkuCmd(ProductSkuCmd cmd);
    public abstract ProductImage fromProductImageCmd(ProductImageCmd cmd);

    @Named("fromProductSkuCmds")
    public List<ProductSku> fromProductSkuCmds(List<ProductSkuCmd> cmds) {
        return cmds.stream()
                .map(this::fromProductSkuCmd)
                .peek(ProductSku::init)
                .toList();
    }

    @Named("fromProductImageCmds")
    public List<ProductImage> fromProductImageCmds(List<ProductImageCmd> cmds) {
        return cmds.stream()
                .map(this::fromProductImageCmd)
                .peek(ProductImage::init)
                .toList();
    }

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

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    @Mapping(target = "minPrice", source = "minPrice")
    @Mapping(target = "maxPrice", source = "maxPrice")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "skuCount", source = "skuCount")
    public abstract StoreProductReviewDto fromStoreProductToPreview(StoreProduct storeProduct);

    // helper mappings used by MapStruct to convert simple types
    protected StoreId map(java.util.UUID id) {
        return id == null ? null : StoreId.of(id);
    }

    protected CategoryId mapCatalog(java.util.UUID id) {
        return id == null ? null : CategoryId.of(id);
    }

    protected SKU mapSku(java.lang.String sku) {
        return sku == null ? null : SKU.of(sku);
    }
}
