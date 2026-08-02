package com.dotran.example.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreConfigRequest {

    private Boolean autoAcceptOrder;

    private Boolean allowPreOrder;

    private LocalTime openingTime;
    private LocalTime closingTime;

    @NotBlank
    private String timeZone;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    @Positive
    private Integer preparationTimeMinutes;

    @Positive
    private Integer maxOrdersPerDay;
}
