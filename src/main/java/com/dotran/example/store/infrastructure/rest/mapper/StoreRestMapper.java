package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.command.CreateStoreCmd;
import com.dotran.example.store.application.command.UpdateBusinessHourCmd;
import com.dotran.example.store.application.command.UpdateStoreConfigCmd;
import com.dotran.example.store.infrastructure.rest.dto.request.AddStoreAvailabilityRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreRestMapper {

    CreateStoreCmd fromRequestToCmd(CreateStoreRequest request);

    UpdateBusinessHourCmd fromRequestToUpdateBusinessHourCmd(UpdateBusinessHourRequest request);

    List<UpdateBusinessHourCmd> fromListRequestToUpdateBusinessHourCmd(List<UpdateBusinessHourRequest> request);

    UpdateStoreConfigCmd fromUpdateStoreConfigToCmd(UpdateStoreConfigRequest updateStoreConfigRequest);

    AddStoreAvailabilityCmd fromRequestToAddStoreAvailabilityCmd(AddStoreAvailabilityRequest request);
}
