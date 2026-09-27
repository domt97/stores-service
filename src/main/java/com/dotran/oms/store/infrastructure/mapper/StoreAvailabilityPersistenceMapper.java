package com.dotran.oms.store.infrastructure.mapper;

import com.dotran.oms.store.common.mapper.InternalIdMapper;
import com.dotran.oms.store.domain.model.StoreAvailability;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        componentModel = "spring",
        uses = InternalIdMapper.class
)
public abstract class StoreAvailabilityPersistenceMapper {

    @Autowired
    protected InternalIdMapper idMapper;

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    public abstract StoreAvailabilityEntity fromDomainToEntity(StoreAvailability storeAvailability);

    @Mapping(target = "id", expression = "java(idMapper.toStoreAvailabilityId(entity.getId()))")
    @Mapping(target = "storeId", expression = "java(idMapper.toStoreId(entity.getStoreId()))")
    public abstract StoreAvailability fromEntityToDomain(StoreAvailabilityEntity entity);
}
