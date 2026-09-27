package com.dotran.oms.store.application.command.store;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddStoreAvailabilityCmd {

    private TenantId tenantId;

    private StoreId storeId;

    private AvailabilityType type;

    private Instant startTime;

    private Instant endTime;

    private String reason;
}
