package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.storeconfig.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.command.store.CreateStoreCmd;
import com.dotran.example.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.example.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreAvailabilityDto;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.infrastructure.rest.dto.request.AddStoreAvailabilityRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreAvailabilityResponse;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreDetailResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreRestMapper {

    CreateStoreCmd fromRequestToCmd(CreateStoreRequest request);

    UpdateBusinessHourCmd fromRequestToUpdateBusinessHourCmd(UpdateBusinessHourRequest request);

    List<UpdateBusinessHourCmd> fromListRequestToUpdateBusinessHourCmd(List<UpdateBusinessHourRequest> request);

    UpdateStoreConfigCmd fromUpdateStoreConfigToCmd(UpdateStoreConfigRequest updateStoreConfigRequest);

    AddStoreAvailabilityCmd fromRequestToAddStoreAvailabilityCmd(AddStoreAvailabilityRequest request);

    StoreDetailResponse toStoreDetailResponse(StoreDetailDto dto);

    StoreAvailabilityResponse toStoreAvailabilityResponse(StoreAvailabilityDto dto);
}
