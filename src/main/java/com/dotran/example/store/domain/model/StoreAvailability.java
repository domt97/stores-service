package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.BaseDomain;
import com.dotran.example.store.common.domain.valueobject.StoreAvailabilityId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.domain.enums.AvailabilityType;
import com.dotran.example.store.domain.exception.BusinessException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class StoreAvailability extends BaseDomain<StoreAvailabilityId> {

    private StoreId storeId;

    private AvailabilityType type;

    private Instant startTime;

    private Instant endTime;

    private String reason;

    private boolean cancelled;

    private Instant createdAt;

    private Instant updatedAt;

    public void generateId() {
        super.id = StoreAvailabilityId.generateId();
    }

    public boolean isActive(Instant now) {

        if (cancelled) {
            return false;
        }

        if (now.isBefore(startTime)) {
            return false;
        }

        return endTime == null || now.isBefore(endTime);
    }

    public void cancel() {
        if (cancelled) {
            throw new BusinessException("This Store Availability config is already cancelled");
        }

        cancelled = true;
        updatedAt = Instant.now();
    }
}
