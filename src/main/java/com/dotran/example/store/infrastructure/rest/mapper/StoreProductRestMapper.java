package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.dto.StoreProductReviewDto;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductPreviewResponse;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreProductResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreProductRestMapper {

    CreateStoreProductCmd fromCreateRequestToCmd(CreateStoreProductRequest request);

    StoreProductResponse toStoreProductResponse(StoreProductDetailDto dto);

    List<StoreProductPreviewResponse> toStoreProductPreviewResponseList(List<StoreProductReviewDto> dtoList);
}
