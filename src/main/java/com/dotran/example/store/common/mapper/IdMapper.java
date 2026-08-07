package com.dotran.example.store.common.mapper;

import com.dotran.example.store.common.domain.valueobject.CustomerId;
import com.dotran.example.store.common.domain.valueobject.StoreAvailabilityId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface IdMapper {

    @Named("toStoreId")
    default StoreId toStoreId(UUID id) {
        return id == null ? null : new StoreId(id);
    }

    @Named("toStoreAvailabilityId")
    default StoreAvailabilityId toStoreAvailabilityId(UUID id) {
        return id == null ? null : new StoreAvailabilityId(id);
    }


    default TenantId toTenantId(UUID id) {
        return id == null ? null : new TenantId(id);
    }

    default CustomerId toCustomerId(UUID id) {
        return id == null ? null : new CustomerId(id);
    }
}
