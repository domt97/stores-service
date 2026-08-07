package com.dotran.example.store.application.dto;

import com.dotran.example.store.domain.enums.AvailabilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAvailabilityDto {

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
