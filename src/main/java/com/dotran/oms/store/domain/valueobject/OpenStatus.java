package com.dotran.oms.store.domain.valueobject;

import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.enums.OpenReason;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a store is open at an instant, and every reason it is not (BR-STO-04): open only when there is no reason.
 * {@code closedWindowTypes} lists the types of the availability windows that close it.
 */
public record OpenStatus(boolean open, Set<OpenReason> reasons, List<AvailabilityType> closedWindowTypes) {

    public OpenStatus {
        // EnumSet iterates in declaration order: the response order is stable on every run.
        reasons = Collections.unmodifiableSet(reasons.isEmpty() ? EnumSet.noneOf(OpenReason.class) : EnumSet.copyOf(reasons));
        closedWindowTypes = List.copyOf(closedWindowTypes);
    }

    /** Reasons in declaration order (stable for the response). */
    public List<OpenReason> orderedReasons() {
        return List.copyOf(reasons);
    }
}
