package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.example.store.application.command.storeproduct.ProductSkuCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.common.mapper.IdMapper;
import com.dotran.example.store.common.utils.CollectionUtils;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateProductSkuRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductPreviewResponse;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = IdMapper.class)
public abstract class StoreProductRestMapper {

    @Autowired
    protected IdMapper idMapper;

    @Mapping(target = "categoryId", expression = "java(idMapper.toCategoryId(request.getCategoryId()))")
    @Mapping(target = "skus", source = "skus", qualifiedByName = "fromCreateProductSkuRequestList")
    public abstract CreateStoreProductCmd fromCreateRequestToCmd(CreateStoreProductRequest request);

    @Mapping(target = "sku", expression = "java(idMapper.toSKU(request.getSku()))")
    public abstract ProductSkuCmd fromCreateProductSkuRequest(CreateProductSkuRequest request);

    @Named("fromCreateProductSkuRequestList")
    public List<ProductSkuCmd> fromCreateProductSkuRequestList(List<CreateProductSkuRequest> request) {
        if (CollectionUtils.isEmpty(request)) {
            return new ArrayList<>();
        }
        return request.stream().map(this::fromCreateProductSkuRequest).toList();
    }

    public abstract StoreProductResponse toStoreProductResponse(StoreProductDetailDto dto);

    public abstract StoreProductPreviewResponse toStoreProductPreviewResponse(StoreProductReviewDto dto);
}
