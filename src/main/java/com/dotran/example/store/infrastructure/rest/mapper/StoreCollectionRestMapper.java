package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.infrastructure.rest.dto.request.UpsertStoreCollectionRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreCollectionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface StoreCollectionRestMapper {

    @Mapping(target = "productIds", source = "productIds", qualifiedByName = "mapProductIds")
    CreateStoreCollectionCmd fromRequestToCreateCmd(UpsertStoreCollectionRequest request);

    @Named("mapProductIds")
    default List<ProductId> mapProductIds(List<UUID> productIds) {
        if (productIds == null) {
            return null;
        }
        List<ProductId> productIdList = new ArrayList<>();
        for (UUID productId : productIds) {
            productIdList.add(ProductId.of(productId));
        }

        return productIdList;
    }

    StoreCollectionResponse fromDtoToResponse(StoreCollectionDto storeCollectionDto);
}
