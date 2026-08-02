package com.dotran.example.store.application.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoreConfigCmd {

    private boolean autoAcceptOrder;
    private boolean allowPreOrder;
    private String timezone;
    private String currency;
    private Integer preparationTimeMinutes;
    private Integer maxOrdersPerDay;
}
