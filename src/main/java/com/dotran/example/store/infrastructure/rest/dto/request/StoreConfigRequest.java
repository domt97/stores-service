package com.dotran.example.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreConfigRequest {

    private boolean autoAcceptOrder;

    private boolean allowPreOrder;

    @NotBlank
    private String timezone;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    @Positive
    private Integer preparationMinutes;

    @Positive
    private Integer maxOrdersPerDay;
}
