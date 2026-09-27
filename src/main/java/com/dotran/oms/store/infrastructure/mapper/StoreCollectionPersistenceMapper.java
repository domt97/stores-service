package com.dotran.oms.store.infrastructure.mapper;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.store.common.mapper.InternalIdMapper;
import com.dotran.oms.store.domain.model.StoreCollection;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreCollectionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = InternalIdMapper.class
)
public abstract class StoreCollectionPersistenceMapper {

    @Autowired
    protected InternalIdMapper idMapper;

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    @Mapping(target = "products", ignore = true)
    public abstract StoreCollectionEntity toBaseEntity(StoreCollection storeCollection);

    @Mapping(target = "id", expression = "java(idMapper.toStoreCollectionId(entity.getId()))")
    @Mapping(target = "storeId", expression = "java(idMapper.toStoreId(entity.getStoreId()))")
    @Mapping(target = "productIds", source = "products", qualifiedByName = "toListProductId")
    public abstract StoreCollection toStoreCollection(StoreCollectionEntity entity);

    @Named("toListProductId")
    public List<ProductId> toListProductId(List<ProductCollectionEntity> productCollections) {
        return productCollections.stream()
                .map(ProductCollectionEntity::getProductId)
                .map(idMapper::toProductId)
                .toList();
    }
}
