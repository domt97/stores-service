package com.dotran.example.store.domain.valueobject;

import com.dotran.example.store.domain.model.BusinessHour;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class StoreConfig {

    private boolean autoAcceptOrder;
    private boolean allowPreOrder;
    private String timezone;
    private String currency;
    private Integer preparationTimeMinutes;
    private Integer maxOrdersPerDay;

    private Instant createdAt;
    private Instant updatedAt;

    public static StoreConfig newConfig(Boolean autoAcceptOrder,
                                        Boolean allowPreOrder,
                                        String timeZone,
                                        String currency,
                                        Integer preparationTimeMinutes,
                                        Integer maxOrdersPerDay) {
        return StoreConfig.builder()
                .allowPreOrder(autoAcceptOrder)
                .allowPreOrder(allowPreOrder)
                .timezone(timeZone)
                .currency(currency)
                .preparationTimeMinutes(preparationTimeMinutes)
                .maxOrdersPerDay(maxOrdersPerDay)
                .createdAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    public boolean isOpen(
            Instant now,
            List<BusinessHour> hours) {
        ZonedDateTime local = now.atZone(ZoneId.of(timezone));
        DayOfWeek day = local.getDayOfWeek();
        LocalTime current = local.toLocalTime();

        return hours.stream()
                .filter(h -> h.getDayOfWeek() == day)
                .findFirst()
                .map(h -> h.isOpen(current))
                .orElse(false);
    }
}
