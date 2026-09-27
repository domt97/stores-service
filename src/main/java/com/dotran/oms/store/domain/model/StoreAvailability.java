package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StoreAvailability extends AggregateRoot<StoreAvailabilityId> {

    private StoreId storeId;
    private AvailabilityType type;
    private Instant startTime;
    private Instant endTime;
    private String reason;
    private boolean cancelled;
    private Instant createdAt;
    private Instant updatedAt;

    public void newStoreAvailability() {
        this.id = StoreAvailabilityId.generateId();
        this.cancelled = false;
        this.createdAt = this.updatedAt = Instant.now();
    }

    public boolean isActive(Instant now) {
        if (cancelled) return false;
        if (now.isBefore(startTime)) return false;
        return endTime == null || now.isBefore(endTime);
    }

    public void cancel() {
        if (cancelled) {
            throw new BusinessException("This Store Availability config is already cancelled");
        }
        this.cancelled = true;
        this.updatedAt = Instant.now();
    }
}
