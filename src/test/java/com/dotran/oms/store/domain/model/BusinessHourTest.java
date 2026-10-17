package com.dotran.oms.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessHour")
class BusinessHourTest {

    @Nested
    @DisplayName("isOpen")
    class IsOpen {

        @Test
        @DisplayName("returns false when day is marked as closed")
        void returnsFalse_whenClosed() {
            BusinessHour bh = BusinessHour.of(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), true);
            assertThat(bh.isOpen(LocalTime.of(12, 0))).isFalse();
        }

        @Test
        @DisplayName("returns true when current time is within business hours")
        void returnsTrue_whenWithinRange() {
            BusinessHour bh = BusinessHour.of(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            assertThat(bh.isOpen(LocalTime.of(12, 0))).isTrue();
        }

        @Test
        @DisplayName("returns false when current time is before opening time")
        void returnsFalse_whenBeforeOpening() {
            BusinessHour bh = BusinessHour.of(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            assertThat(bh.isOpen(LocalTime.of(8, 59))).isFalse();
        }

        @Test
        @DisplayName("returns false when current time is after closing time")
        void returnsFalse_whenAfterClosing() {
            BusinessHour bh = BusinessHour.of(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            assertThat(bh.isOpen(LocalTime.of(18, 1))).isFalse();
        }

        @Test
        @DisplayName("returns true when current time exactly equals opening time")
        void returnsTrue_whenExactlyAtOpeningTime() {
            BusinessHour bh = BusinessHour.of(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            assertThat(bh.isOpen(LocalTime.of(9, 0))).isTrue();
        }

        @Test
        @DisplayName("returns true when current time exactly equals closing time")
        void returnsTrue_whenExactlyAtClosingTime() {
            BusinessHour bh = BusinessHour.of(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            assertThat(bh.isOpen(LocalTime.of(18, 0))).isTrue();
        }
    }

    @Nested
    @DisplayName("withUpdated")
    class WithUpdated {

        @Test
        @DisplayName("preserves the original day of week and applies new values")
        void preservesDayAndUpdatesValues() {
            BusinessHour original = BusinessHour.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            BusinessHour updated = original.withUpdated(LocalTime.of(10, 0), LocalTime.of(20, 0), true);

            assertThat(updated.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
            assertThat(updated.getOpeningTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(updated.getClosingTime()).isEqualTo(LocalTime.of(20, 0));
            assertThat(updated.isClosed()).isTrue();
        }

        @Test
        @DisplayName("does not mutate the original instance")
        void doesNotMutateOriginal() {
            BusinessHour original = BusinessHour.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false);
            original.withUpdated(LocalTime.of(10, 0), LocalTime.of(20, 0), true);

            assertThat(original.getOpeningTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(original.getClosingTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(original.isClosed()).isFalse();
        }
    }

    @Nested
    @DisplayName("defaultConfig")
    class DefaultConfig {

        @Test
        @DisplayName("creates one entry for each day of the week")
        void createsSevenEntries() {
            List<BusinessHour> defaults = BusinessHour.defaultConfig();
            assertThat(defaults).hasSize(7);
        }

        @Test
        @DisplayName("defaults all days to 9:00-18:00, open")
        void defaultsToNineToEighteen() {
            List<BusinessHour> defaults = BusinessHour.defaultConfig();
            assertThat(defaults).allSatisfy(bh -> {
                assertThat(bh.getOpeningTime()).isEqualTo(LocalTime.of(9, 0));
                assertThat(bh.getClosingTime()).isEqualTo(LocalTime.of(18, 0));
                assertThat(bh.isClosed()).isFalse();
            });
        }

        @Test
        @DisplayName("covers all seven days of the week")
        void coversAllDaysOfWeek() {
            List<BusinessHour> defaults = BusinessHour.defaultConfig();
            assertThat(defaults)
                    .extracting(BusinessHour::getDayOfWeek)
                    .containsExactlyInAnyOrderElementsOf(List.of(DayOfWeek.values()));
        }
    }
}
