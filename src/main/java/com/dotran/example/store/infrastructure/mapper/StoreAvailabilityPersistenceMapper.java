package com.dotran.example.store.infrastructure.mapper;

import com.dotran.example.store.common.mapper.IdMapper;
import com.dotran.example.store.domain.model.StoreAvailability;
import com.dotran.example.store.infrastructure.persistence.entity.StoreAvailabilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        componentModel = "spring",
        uses = IdMapper.class
)
public abstract class StoreAvailabilityPersistenceMapper {

    @Autowired
    protected IdMapper idMapper;

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    public abstract StoreAvailabilityEntity fromDomainToEntity(StoreAvailability storeAvailability);

    @Mapping(target = "id", expression = "java(idMapper.toStoreAvailabilityId(entity.getId()))")
    @Mapping(target = "storeId", expression = "java(idMapper.toStoreId(entity.getStoreId()))")
    public abstract StoreAvailability fromEntityToDomain(StoreAvailabilityEntity entity);
}
