package com.dotran.oms.store.application.mapper;

import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.domain.model.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class MerchantDataMapper {

    @Mapping(target = "id", source = "id.value")
    public abstract MerchantDto toMerchantDto(Merchant merchant);
}
