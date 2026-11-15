package com.dotran.oms.store.infrastructure.mapper;

import com.dotran.oms.store.common.mapper.InternalIdMapper;
import com.dotran.oms.store.domain.model.Merchant;
import com.dotran.oms.store.infrastructure.persistence.entity.MerchantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        componentModel = "spring",
        uses = InternalIdMapper.class
)
public abstract class MerchantPersistenceMapper {

    @Autowired
    protected InternalIdMapper idMapper;

    @Mapping(target = "id", source = "id.value")
    public abstract MerchantEntity fromMerchantToEntity(Merchant merchant);

    @Mapping(target = "id", expression = "java(idMapper.toMerchantId(merchantEntity.getId()))")
    public abstract Merchant fromEntityToMerchant(MerchantEntity merchantEntity);
}
