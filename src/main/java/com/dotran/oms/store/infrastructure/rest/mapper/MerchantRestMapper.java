package com.dotran.oms.store.infrastructure.rest.mapper;

import com.dotran.oms.store.application.command.merchant.CreateMerchantCmd;
import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.infrastructure.rest.dto.request.CreateMerchantRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.MerchantResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MerchantRestMapper {

    public abstract CreateMerchantCmd fromRequestToCmd(CreateMerchantRequest request);

    public abstract MerchantResponse toMerchantResponse(MerchantDto dto);
}
