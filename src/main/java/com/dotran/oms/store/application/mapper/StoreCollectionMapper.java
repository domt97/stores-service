package com.dotran.oms.store.application.mapper;

import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.common.mapper.InternalIdMapper;
import com.dotran.oms.store.domain.model.StoreCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {InternalIdMapper.class})
public abstract class StoreCollectionMapper {

    @Mapping(target = "productIds", ignore = true)
    public abstract StoreCollection fromCreateCmd(CreateStoreCollectionCmd cmd);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    @Mapping(target = "productCount", source = "productCount")
    public abstract StoreCollectionDto toCollectionDto(StoreCollection storeCollection);

}
