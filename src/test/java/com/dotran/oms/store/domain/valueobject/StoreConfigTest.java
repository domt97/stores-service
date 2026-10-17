package com.dotran.oms.store.domain.valueobject;

import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.domain.model.BusinessHour;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoreConfig")
class StoreConfigTest {

    private StoreConfig baseConfig() {
        return StoreConfig.builder()
                .autoAcceptOrder(true)
                .allowPreOrder(false)
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .timeZone("UTC")
                .currency("USD")
                .preparationTimeMinutes(30)
                .maxOrdersPerDay(100)
                .build();
    }

    @Nested
    @DisplayName("withUpdated")
    class WithUpdated {

        @Test
        @DisplayName("throws BusinessException when openingTime is after closingTime")
        void throwsException_whenOpeningAfterClosing() {
            StoreConfig config = baseConfig();
            assertThatThrownBy(() -> config.withUpdated(
                    true, false,
                    LocalTime.of(18, 0), LocalTime.of(9, 0),
                    "UTC", "USD", 30, 100))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws BusinessException when openingTime equals closingTime")
        void throwsException_whenOpeningEqualsClosing() {
            StoreConfig config = baseConfig();
            assertThatThrownBy(() -> config.withUpdated(
                    true, false,
                    LocalTime.of(9, 0), LocalTime.of(9, 0),
                    "UTC", "USD", 30, 100))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("skips time validation when both times are null")
        void skipsValidation_whenTimesAreNull() {
            StoreConfig config = baseConfig();
            StoreConfig updated = config.withUpdated(false, true, null, null, "Asia/Ho_Chi_Minh", "VND", 15, 50);
            assertThat(updated.getCurrency()).isEqualTo("VND");
            assertThat(updated.isAllowPreOrder()).isTrue();
        }

        @Test
        @DisplayName("returns a new instance with updated values")
        void returnsNewInstance_withUpdatedValues() {
            StoreConfig config = baseConfig();
            StoreConfig updated = config.withUpdated(
                    false, true, LocalTime.of(8, 0), LocalTime.of(22, 0), "UTC", "EUR", 15, 50);

            assertThat(updated).isNotSameAs(config);
            assertThat(updated.isAutoAcceptOrder()).isFalse();
            assertThat(updated.isAllowPreOrder()).isTrue();
            assertThat(updated.getOpeningTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(updated.getClosingTime()).isEqualTo(LocalTime.of(22, 0));
            assertThat(updated.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("does not mutate the original instance")
        void doesNotMutateOriginal() {
            StoreConfig config = baseConfig();
            config.withUpdated(false, true, LocalTime.of(8, 0), LocalTime.of(22, 0), "UTC", "EUR", 15, 50);

            assertThat(config.getCurrency()).isEqualTo("USD");
            assertThat(config.isAutoAcceptOrder()).isTrue();
        }
    }

    @Nested
    @DisplayName("isOpen")
    class IsOpen {

        // Wednesday 2024-01-03T10:00:00Z
        private final Instant wednesdayAt10AM = Instant.parse("2024-01-03T10:00:00Z");

        @Test
        @DisplayName("returns true when the current day and time fall within open business hours")
        void returnsTrue_whenDayIsOpen() {
            StoreConfig config = baseConfig(); // UTC, 9:00-18:00
            List<BusinessHour> hours = List.of(
                    BusinessHour.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false));
            assertThat(config.isOpen(wednesdayAt10AM, hours)).isTrue();
        }

        @Test
        @DisplayName("returns false when the business hour for the current day is marked closed")
        void returnsFalse_whenDayIsClosed() {
            StoreConfig config = baseConfig(); // UTC
            List<BusinessHour> hours = List.of(
                    BusinessHour.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
            assertThat(config.isOpen(wednesdayAt10AM, hours)).isFalse();
        }

        @Test
        @DisplayName("returns false when no business hour entry exists for the current day")
        void returnsFalse_whenNoDayEntry() {
            StoreConfig config = baseConfig(); // UTC
            List<BusinessHour> hours = List.of(
                    BusinessHour.of(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false));
            assertThat(config.isOpen(wednesdayAt10AM, hours)).isFalse();
        }

        @Test
        @DisplayName("returns false when current time is outside business hours")
        void returnsFalse_whenOutsideHours() {
            StoreConfig config = baseConfig(); // UTC, 9:00-18:00
            // Wednesday at 20:00 UTC
            Instant wednesdayAt8PM = Instant.parse("2024-01-03T20:00:00Z");
            List<BusinessHour> hours = List.of(
                    BusinessHour.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false));
            assertThat(config.isOpen(wednesdayAt8PM, hours)).isFalse();
        }
    }
}
