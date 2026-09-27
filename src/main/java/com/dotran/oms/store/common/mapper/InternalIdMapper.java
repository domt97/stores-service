package com.dotran.oms.store.common.mapper;

import com.dotran.oms.core.mapper.IdMapper;
import com.dotran.oms.store.common.id.ProductImageId;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.common.id.StoreCollectionId;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface InternalIdMapper extends IdMapper {

    @Named("toStoreAvailabilityId")
    default StoreAvailabilityId toStoreAvailabilityId(UUID id) {
        return id == null ? null : new StoreAvailabilityId(id);
    }

    @Named("toProductSkuId")
    default ProductSkuId toProductSkuId(UUID id) {
        return id == null ? null : new ProductSkuId(id);
    }

    @Named("toProductImageId")
    default ProductImageId toProductImageId(Long id) {
        return id == null ? null : new ProductImageId(id);
    }

    @Named("toStoreCollectionId")
    default StoreCollectionId toStoreCollectionId(UUID id) {
        return id == null ? null : new StoreCollectionId(id);
    }
}
