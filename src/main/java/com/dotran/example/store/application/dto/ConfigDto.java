package com.dotran.example.store.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigDto {

    private boolean autoAcceptOrder;
    private boolean allowPreOrder;
    private String timezone;
    private String currency;
    private Integer preparationTimeMinutes;
    private Integer maxOrdersPerDay;
}
