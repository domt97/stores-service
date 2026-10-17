package com.dotran.oms.store.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessHour {

    private final DayOfWeek dayOfWeek;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final boolean closed;

    public static BusinessHour of(DayOfWeek dayOfWeek,
                                  LocalTime openingTime,
                                  LocalTime closingTime,
                                  boolean closed) {
        return BusinessHour.builder()
                .dayOfWeek(dayOfWeek)
                .openingTime(openingTime)
                .closingTime(closingTime)
                .closed(closed)
                .build();
    }

    public static List<BusinessHour> defaultConfig() {
        return Arrays.stream(DayOfWeek.values())
                .map(dow -> BusinessHour.of(dow, LocalTime.of(9, 0), LocalTime.of(18, 0), false))
                .collect(Collectors.toList());
    }

    public BusinessHour withUpdated(LocalTime openingTime, LocalTime closingTime, boolean closed) {
        return BusinessHour.builder()
                .dayOfWeek(this.dayOfWeek)
                .openingTime(openingTime)
                .closingTime(closingTime)
                .closed(closed)
                .build();
    }

    public boolean isOpen(LocalTime current) {
        if (closed) return false;
        return !current.isBefore(openingTime) && !current.isAfter(closingTime);
    }
}
