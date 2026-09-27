package com.dotran.oms.store.application.command.store;

import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.application.command.common.AddressCmd;
import com.dotran.oms.store.application.command.storeconfig.BusinessHourCmd;
import com.dotran.oms.store.application.command.storeconfig.StoreConfigCmd;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
