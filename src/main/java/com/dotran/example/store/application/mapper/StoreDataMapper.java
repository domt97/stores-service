package com.dotran.example.store.application.mapper;

import com.dotran.example.store.application.command.AddressCmd;
import com.dotran.example.store.application.command.BusinessHourCmd;
import com.dotran.example.store.application.command.StoreConfigCmd;
import com.dotran.example.store.application.command.UpdateBusinessHourCmd;
import com.dotran.example.store.application.dto.BusinessHourDto;
import com.dotran.example.store.application.dto.ConfigDto;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.common.domain.valueobject.Address;
import com.dotran.example.store.common.dto.AddressDto;
import com.dotran.example.store.domain.model.BusinessHour;
import com.dotran.example.store.domain.model.Store;
import com.dotran.example.store.domain.valueobject.StoreConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class StoreDataMapper {

    public abstract Address fromAddressCmdToAddress(AddressCmd cmd);

    public abstract StoreConfig fromStoreConfigCmdToStoreConfig(StoreConfigCmd cmd);

    public abstract BusinessHour fromBusinessHourCmdToBusinessHour(BusinessHourCmd businessHourCmd);

    public abstract List<BusinessHour> fromListBusinessHourCmdToListBusinessHour(List<BusinessHourCmd> cmds);

    public abstract BusinessHour fromUpdateBusinessHourCmdToBusinessHour(UpdateBusinessHourCmd updateBusinessHourCmd);

    public abstract List<BusinessHour> fromListUpdateBusinessHourCmdToListBusinessHour(List<UpdateBusinessHourCmd> updateBusinessHourCmds);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "tenantId", source = "tenantId.value")
    @Mapping(target = "ownerId", source = "ownerId.value")
    @Mapping(target = "address", source = "address", qualifiedByName = "toAddressDto")
    @Mapping(target = "config", source = "config", qualifiedByName = "toConfigDto")
    @Mapping(target = "businessHours", source = "businessHours", qualifiedByName = "toListBusinessHourDto")
    public abstract StoreDetailDto toStoreDetailDto(Store store);

    @Named("toAddressDto")
    public abstract AddressDto toAddressDto(Address address);

    @Named("toConfigDto")
    public abstract ConfigDto toConfigDto(StoreConfig storeConfig);

    @Named("toBusinessHourDto")
    public abstract BusinessHourDto toBusinessHourDto(BusinessHour businessHour);

    @Named("toListBusinessHourDto")
    public abstract List<BusinessHourDto> toListBusinessHourDto(List<BusinessHour> businessHours);
}
