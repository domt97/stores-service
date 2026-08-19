package com.dotran.example.store.infrastructure.rest.dto.response;

import com.dotran.example.store.domain.enums.AvailabilityType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class StoreAvailabilityResponse {

    private UUID id;
    private UUID storeId;
    private AvailabilityType type;
    private Instant startTime;
    private Instant endTime;
    private String reason;
    private boolean cancelled;
    private Instant createdAt;
    private Instant updatedAt;
}
