package com.dotran.example.store.application.command.storeconfig;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class StoreConfigCmd {

    private boolean autoAcceptOrder;
    private boolean allowPreOrder;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String timeZone;
    private String currency;
    private Integer preparationTimeMinutes;
    private Integer maxOrdersPerDay;
}
