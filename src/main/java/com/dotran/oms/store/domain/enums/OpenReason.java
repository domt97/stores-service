package com.dotran.oms.store.domain.enums;

/** Why a store is not open at an instant (BR-STO-02, BR-STO-04). */
public enum OpenReason {
    /** The store status is not ACTIVE: only ACTIVE stores take orders. */
    STORE_NOT_ACTIVE,
    /** The local time is outside today's business hours, or today is a closed day. */
    OUTSIDE_HOURS,
    /** An availability window (closure, holiday, maintenance, ...) covers the instant. */
    CLOSED_WINDOW,
    /** The time zone or today's business hours cannot be read: treated as closed (BR-STO-04, decided 2026-11-15). */
    INVALID_SCHEDULE
}
