package com.dotran.example.store.application.mapper;

import com.dotran.example.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.common.mapper.IdMapper;
import com.dotran.example.store.domain.model.StoreCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {IdMapper.class})
public abstract class StoreCollectionMapper {

    @Mapping(target = "productIds", ignore = true)
    public abstract StoreCollection fromCreateCmd(CreateStoreCollectionCmd cmd);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    @Mapping(target = "productCount", source = "productCount")
    public abstract StoreCollectionDto toCollectionDto(StoreCollection storeCollection);

}
