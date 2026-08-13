package com.dotran.example.store.infrastructure.mapper;

import com.dotran.example.store.common.mapper.IdMapper;
import com.dotran.example.store.domain.model.BusinessHour;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.domain.valueobject.StoreConfig;
import com.dotran.example.store.common.domain.valueobject.Address;
import com.dotran.example.store.infrastructure.persistence.entity.BusinessHourEntity;
import com.dotran.example.store.infrastructure.persistence.entity.StoreAddressEntity;
import com.dotran.example.store.infrastructure.persistence.entity.StoreConfigEntity;
import com.dotran.example.store.infrastructure.persistence.entity.StoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = IdMapper.class
)
public abstract class StorePersistenceMapper {

    @Autowired
    protected IdMapper idMapper;

    //
    // Domain -> Entity
    //

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "tenantId", source = "tenantId.value")
    @Mapping(target = "ownerId", source = "ownerId.value")
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "config", ignore = true)
    @Mapping(target = "businessHours", ignore = true)
    public abstract StoreEntity fromStoreToEntity(Store store);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", source = "tenantId.value")
    @Mapping(target = "ownerId", source = "ownerId.value")
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "config", ignore = true)
    @Mapping(target = "businessHours", ignore = true)
    public abstract void updateStoreEntity(@MappingTarget StoreEntity entity, Store store);

    @Named("fromAddressToStoreAddressEntity")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "phone", source = "address.phone")
    @Mapping(target = "store", source = "storeEntity")
    public abstract StoreAddressEntity fromAddressToStoreAddressEntity(Address address, StoreEntity storeEntity);

    @Named("updateStoreAddress")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    public abstract void updateStoreAddress(@MappingTarget StoreAddressEntity entity, Address address);

    @Named("fromStoreConfigToStoreConfigEntity")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", source = "storeEntity")
    @Mapping(target = "createdAt", source = "storeConfig.createdAt")
    @Mapping(target = "updatedAt", source = "storeConfig.updatedAt")
    public abstract StoreConfigEntity fromStoreConfigToStoreConfigEntity(StoreConfig storeConfig, StoreEntity storeEntity);

    @Named("updateStoreConfig")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract void updateStoreConfig(@MappingTarget StoreConfigEntity entity, StoreConfig storeConfig);

    @Mapping(target = "id", source = "businessHour.id")
    @Mapping(target = "store", source = "storeEntity")
    public abstract BusinessHourEntity fromBusinessHourToEntity(BusinessHour businessHour, StoreEntity storeEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    public abstract void updateBusinessHourEntity(@MappingTarget BusinessHourEntity entity, BusinessHour businessHour);

    public List<BusinessHourEntity> fromBusinessHourListToEntities(List<BusinessHour> businessHours, StoreEntity storeEntity) {
        if (null == businessHours || businessHours.isEmpty()) {
            return new ArrayList<>();
        }
        return businessHours.stream()
                .map(bh -> this.fromBusinessHourToEntity(bh, storeEntity))
                .collect(Collectors.toList());
    }

    public void updateBusinessHours(List<BusinessHourEntity> businessHourEntities, List<BusinessHour> businessHours) {
        Map<Long, BusinessHour> businessHourMap = businessHours.stream()
                .collect(Collectors.toMap(BusinessHour::getId, Function.identity()));

        for (BusinessHourEntity businessHourEntity : businessHourEntities) {
            BusinessHour businessHour = businessHourMap.get(businessHourEntity.getId());
            if (null != businessHour) {
                this.updateBusinessHourEntity(businessHourEntity, businessHour);
            }
        }
    }

    //
    // Entity -> Domain
    //

    @Mapping(target = "id", expression = "java(idMapper.toStoreId(storeEntity.getId()))")
    @Mapping(target = "tenantId", expression = "java(idMapper.toTenantId(storeEntity.getTenantId()))")
    @Mapping(target = "ownerId", expression = "java(idMapper.toCustomerId(storeEntity.getOwnerId()))")
    @Mapping(target = "address", source = "address", qualifiedByName = "fromStoreAddressEntityToAddress")
    @Mapping(target = "config", source = "config", qualifiedByName = "fromStoreConfigEntityToStoreConfig")
    @Mapping(target = "businessHours", source = "businessHours", qualifiedByName = "fromListBusinessHourEntityToBusinessHours")
    public abstract Store fromEntityToStore(StoreEntity storeEntity);

    @Named("fromStoreAddressEntityToAddress")
    public abstract Address fromStoreAddressEntityToAddress(StoreAddressEntity storeAddressEntity);

    @Named("fromStoreConfigEntityToStoreConfig")
    public abstract StoreConfig fromStoreConfigEntityToStoreConfig(StoreConfigEntity storeConfigEntity);

    @Named("fromBusinessHourEntityToBusinessHour")
    public abstract BusinessHour fromBusinessHourEntityToBusinessHour(BusinessHourEntity businessHour);

    @Named("fromListBusinessHourEntityToBusinessHours")
    public List<BusinessHour> fromListBusinessHourEntityToBusinessHours(List<BusinessHourEntity> businessHourEntities) {
        if (null == businessHourEntities || businessHourEntities.isEmpty()) {
            return new ArrayList<>();
        }
        return businessHourEntities.stream()
                .map(this::fromBusinessHourEntityToBusinessHour)
                .collect(Collectors.toList());
    }
}
