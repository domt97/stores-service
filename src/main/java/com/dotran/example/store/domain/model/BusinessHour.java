package com.dotran.example.store.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class BusinessHour {

    private Long id;
    private DayOfWeek dayOfWeek;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private boolean closed;

    public BusinessHour newBusinessHourConfig(DayOfWeek dayOfWeek,
                                              LocalTime openingTime,
                                              LocalTime closingTime,
                                              Boolean isClosed) {
        return BusinessHour.builder()
                .dayOfWeek(dayOfWeek)
                .openingTime(openingTime)
                .closingTime(closingTime)
                .closed(isClosed)
                .build();
    }

    public void updateBusinessHour(DayOfWeek dayOfWeek,
                                   LocalTime openingTime,
                                   LocalTime closingTime,
                                   boolean isClosed) {
        this.dayOfWeek = dayOfWeek;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.closed = isClosed;
    }

    public List<BusinessHour> defaultBusinessHourConfig() {
        return Arrays.stream(DayOfWeek.values())
                .map(dow -> this.newBusinessHourConfig(dow,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        false))
                .collect(Collectors.toList());
    }

    public boolean isOpen(LocalTime current) {
        if (closed) {
            return false;
        }

        return !current.isBefore(openingTime)
                && !current.isAfter(closingTime);
    }
}
