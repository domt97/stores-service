package com.dotran.example.store.application.command.store;

import com.dotran.example.store.application.command.common.AddressCmd;
import com.dotran.example.store.application.command.storeconfig.BusinessHourCmd;
import com.dotran.example.store.application.command.storeconfig.StoreConfigCmd;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreateStoreCmd {

    private UUID tenantId;

    private UUID storeId;

    private String code;

    private String name;

    private UUID ownerId;

    private String email;

    private String phone;

    private AddressCmd address;

    private StoreConfigCmd config;

    private List<BusinessHourCmd> businessHours;
}
