package com.dotran.oms.store.application.mapper;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.valueobject.Address;
import com.dotran.oms.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.oms.store.application.command.common.AddressCmd;
import com.dotran.oms.store.application.command.storeconfig.BusinessHourCmd;
import com.dotran.oms.store.application.command.storeconfig.StoreConfigCmd;
import com.dotran.oms.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.oms.store.application.dto.BusinessHourDto;
import com.dotran.oms.store.application.dto.ConfigDto;
import com.dotran.oms.store.application.dto.StoreAvailabilityDto;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.common.dto.AddressDto;
import com.dotran.oms.store.domain.model.BusinessHour;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreAvailability;
import com.dotran.oms.store.domain.valueobject.StoreConfig;
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

    @Mapping(target = "storeId", source = "storeId")
    @Mapping(target = "cancelled", constant = "false")
    public abstract StoreAvailability fromCmdToStoreAvailability(AddStoreAvailabilityCmd cmd, StoreId storeId);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "tenantId", source = "tenantId.value")
    @Mapping(target = "ownerId", source = "ownerId.value")
    @Mapping(target = "address", source = "address", qualifiedByName = "toAddressDto")
    @Mapping(target = "config", source = "config", qualifiedByName = "toConfigDto")
    @Mapping(target = "businessHours", source = "businessHours", qualifiedByName = "toListBusinessHourDto")
    public abstract StoreDetailDto toStoreDetailDto(Store store);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "storeId", source = "storeId.value")
    public abstract StoreAvailabilityDto toStoreAvailabilityDto(StoreAvailability storeAvailability);

    @Named("toAddressDto")
    public abstract AddressDto toAddressDto(Address address);

    @Named("toConfigDto")
    public abstract ConfigDto toConfigDto(StoreConfig storeConfig);

    @Named("toBusinessHourDto")
    public abstract BusinessHourDto toBusinessHourDto(BusinessHour businessHour);

    @Named("toListBusinessHourDto")
    public abstract List<BusinessHourDto> toListBusinessHourDto(List<BusinessHour> businessHours);
}
