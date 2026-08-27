package com.dotran.example.store.application.command.store;

import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.enums.AvailabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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
