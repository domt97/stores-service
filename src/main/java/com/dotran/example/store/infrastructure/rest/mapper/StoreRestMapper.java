package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.command.store.CloseStoreCmd;
import com.dotran.example.store.application.command.store.CreateStoreCmd;
import com.dotran.example.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.example.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreAvailabilityDto;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.common.mapper.IdMapper;
import com.dotran.example.store.infrastructure.rest.dto.request.AddStoreAvailabilityRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreAvailabilityResponse;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = IdMapper.class)
public abstract class StoreRestMapper {

    @Autowired
    protected IdMapper idMapper;

    @Mapping(target = "tenantId", expression = "java(idMapper.toTenantId(request.getTenantId()))")
    @Mapping(target = "ownerId", expression = "java(idMapper.toCustomerId(request.getOwnerId()))")
    public abstract CreateStoreCmd fromRequestToCmd(CreateStoreRequest request);

    @Mapping(target = "storeId", expression = "java(idMapper.toStoreId(storeId))")
    @Mapping(target = "tenantId", expression = "java(idMapper.toTenantId(tenantId))")
    public abstract CloseStoreCmd fromRequestToCloseStoreCmd(UUID tenantId, UUID storeId);

    public abstract UpdateBusinessHourCmd fromRequestToUpdateBusinessHourCmd(UpdateBusinessHourRequest request);

    public abstract List<UpdateBusinessHourCmd> fromListRequestToUpdateBusinessHourCmd(List<UpdateBusinessHourRequest> request);

    public abstract UpdateStoreConfigCmd fromUpdateStoreConfigToCmd(UpdateStoreConfigRequest updateStoreConfigRequest);

    public abstract AddStoreAvailabilityCmd fromRequestToAddStoreAvailabilityCmd(AddStoreAvailabilityRequest request);

    public abstract StoreDetailResponse toStoreDetailResponse(StoreDetailDto dto);

    public abstract StoreAvailabilityResponse toStoreAvailabilityResponse(StoreAvailabilityDto dto);
}
