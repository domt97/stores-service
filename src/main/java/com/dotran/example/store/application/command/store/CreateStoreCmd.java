package com.dotran.example.store.application.command.store;

import com.dotran.example.store.application.command.common.AddressCmd;
import com.dotran.example.store.application.command.storeconfig.BusinessHourCmd;
import com.dotran.example.store.application.command.storeconfig.StoreConfigCmd;
import com.dotran.example.store.common.domain.valueobject.CustomerId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreateStoreCmd {

    private TenantId tenantId;

    private String code;

    private String name;

    private CustomerId ownerId;

    private String email;

    private String phone;

    private AddressCmd address;

    private StoreConfigCmd config;

    private List<BusinessHourCmd> businessHours;
}
