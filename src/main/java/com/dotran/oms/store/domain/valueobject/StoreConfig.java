package com.dotran.oms.store.domain.valueobject;

import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.domain.model.BusinessHour;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StoreConfig {

    private final boolean autoAcceptOrder;
    private final boolean allowPreOrder;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final String timeZone;
    private final String currency;
    private final Integer preparationTimeMinutes;
    private final Integer maxOrdersPerDay;

    public StoreConfig withUpdated(Boolean autoAcceptOrder,
                                   Boolean allowPreOrder,
                                   LocalTime openingTime,
                                   LocalTime closingTime,
                                   String timeZone,
                                   String currency,
                                   Integer preparationTimeMinutes,
                                   Integer maxOrdersPerDay) {
        if (null != openingTime && null != closingTime) {
            if (openingTime.isAfter(closingTime) || openingTime.equals(closingTime)) {
                throw new BusinessException("Opening time must be before closing time");
            }
        }
        return StoreConfig.builder()
                .autoAcceptOrder(autoAcceptOrder)
                .allowPreOrder(allowPreOrder)
                .openingTime(openingTime)
                .closingTime(closingTime)
                .timeZone(timeZone)
                .currency(currency)
                .preparationTimeMinutes(preparationTimeMinutes)
                .maxOrdersPerDay(maxOrdersPerDay)
                .build();
    }

    public boolean isOpen(Instant now, List<BusinessHour> hours) {
        ZonedDateTime local = now.atZone(ZoneId.of(timeZone));
        DayOfWeek day = local.getDayOfWeek();
        LocalTime current = local.toLocalTime();

        return hours.stream()
                .filter(h -> h.getDayOfWeek() == day)
                .findFirst()
                .map(h -> h.isOpen(current))
                .orElse(false);
    }
}
