package com.dotran.example.store.infrastructure.rest.dto.request;

import com.dotran.example.store.domain.enums.AvailabilityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStoreAvailabilityRequest {

    @NotNull(message = "Type is required")
    private AvailabilityType type;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    private Instant endTime;

    private String reason;
}
