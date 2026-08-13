package com.dotran.example.store.domain.valueobject;

import com.dotran.example.store.domain.exception.BusinessException;
import com.dotran.example.store.domain.model.BusinessHour;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreConfig {

    private boolean autoAcceptOrder;
    private boolean allowPreOrder;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String timeZone;
    private String currency;
    private Integer preparationTimeMinutes;
    private Integer maxOrdersPerDay;
    private Instant createdAt;
    private Instant updatedAt;

    public static StoreConfig newConfig(Boolean autoAcceptOrder,
                                        Boolean allowPreOrder,
                                        LocalTime openingTime,
                                        LocalTime closingTime,
                                        String timeZone,
                                        String currency,
                                        Integer preparationTimeMinutes,
                                        Integer maxOrdersPerDay) {
        return StoreConfig.builder()
                .autoAcceptOrder(autoAcceptOrder)
                .allowPreOrder(allowPreOrder)
                .openingTime(openingTime)
                .closingTime(closingTime)
                .timeZone(timeZone)
                .currency(currency)
                .preparationTimeMinutes(preparationTimeMinutes)
                .maxOrdersPerDay(maxOrdersPerDay)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void updateConfig(Boolean autoAcceptOrder,
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
        this.autoAcceptOrder = autoAcceptOrder;
        this.allowPreOrder = allowPreOrder;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.timeZone = timeZone;
        this.currency = currency;
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.maxOrdersPerDay = maxOrdersPerDay;
        this.updatedAt = Instant.now();
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
