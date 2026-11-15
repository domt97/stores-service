package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.enums.OpenReason;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.valueobject.OpenStatus;
import com.dotran.oms.store.domain.valueobject.StoreConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Store.openAt (BR-STO-02, BR-STO-04)")
class StoreOpenAtTest {

    private static final String HCM = "Asia/Ho_Chi_Minh";
    /** Monday 2026-11-16 10:00 in Ho Chi Minh City (UTC+7). */
    private static final Instant MONDAY_10_LOCAL = Instant.parse("2026-11-16T03:00:00Z");
    /** Monday 2026-11-16 19:00 local. */
    private static final Instant MONDAY_19_LOCAL = Instant.parse("2026-11-16T12:00:00Z");

    private static BusinessHour monday(LocalTime open, LocalTime close, boolean closed) {
        return BusinessHour.builder().dayOfWeek(DayOfWeek.MONDAY).openingTime(open).closingTime(close).closed(closed).build();
    }

    private static Store store(StoreStatus status, String timeZone, List<BusinessHour> hours) {
        return Store.builder()
                .id(StoreId.newStoreId())
                .merchantId(MerchantId.newMerchantId())
                .name("Store").code("S1").ownerId(CustomerId.newCustomerId())
                .status(status)
                .config(timeZone == null ? null : StoreConfig.builder().timeZone(timeZone).currency("VND").build())
                .businessHours(hours == null ? null : new ArrayList<>(hours))
                .build();
    }

    private static Store openMonday(StoreStatus status) {
        return store(status, HCM, List.of(monday(LocalTime.of(8, 0), LocalTime.of(18, 0), false)));
    }

    private static StoreAvailability window(AvailabilityType type) {
        return StoreAvailability.builder().id(StoreAvailabilityId.generateId())
                .storeId(StoreId.newStoreId()).type(type)
                .startTime(MONDAY_10_LOCAL.minusSeconds(600)).build();
    }

    @Test
    @DisplayName("ACTIVE, inside today's hours, no window → open, no reason")
    void open() {
        OpenStatus s = openMonday(StoreStatus.ACTIVE).openAt(MONDAY_10_LOCAL, List.of());
        assertThat(s.open()).isTrue();
        assertThat(s.reasons()).isEmpty();
        assertThat(s.closedWindowTypes()).isEmpty();
    }

    @Test
    @DisplayName("outside today's hours (local time, not UTC) → OUTSIDE_HOURS")
    void outsideHours() {
        OpenStatus s = openMonday(StoreStatus.ACTIVE).openAt(MONDAY_19_LOCAL, List.of());
        assertThat(s.open()).isFalse();
        assertThat(s.reasons()).containsExactly(OpenReason.OUTSIDE_HOURS);
    }

    @Test
    @DisplayName("today marked closed, or no business hour for today → OUTSIDE_HOURS")
    void closedDayOrNoHours() {
        Store closedDay = store(StoreStatus.ACTIVE, HCM, List.of(monday(LocalTime.of(8, 0), LocalTime.of(18, 0), true)));
        Store noHours = store(StoreStatus.ACTIVE, HCM, List.of());
        assertThat(closedDay.openAt(MONDAY_10_LOCAL, List.of()).reasons()).containsExactly(OpenReason.OUTSIDE_HOURS);
        assertThat(noHours.openAt(MONDAY_10_LOCAL, List.of()).reasons()).containsExactly(OpenReason.OUTSIDE_HOURS);
    }

    @ParameterizedTest
    @EnumSource(value = StoreStatus.class, names = {"INACTIVE", "SUSPENDED", "CLOSED"})
    @DisplayName("status not ACTIVE → STORE_NOT_ACTIVE, hours still evaluated")
    void notActive(StoreStatus status) {
        assertThat(openMonday(status).openAt(MONDAY_10_LOCAL, List.of()).reasons())
                .containsExactly(OpenReason.STORE_NOT_ACTIVE);
        assertThat(openMonday(status).openAt(MONDAY_19_LOCAL, List.of()).reasons())
                .containsExactly(OpenReason.STORE_NOT_ACTIVE, OpenReason.OUTSIDE_HOURS);
    }

    @Test
    @DisplayName("every window passed in closes the store and is reported with its type")
    void windows() {
        OpenStatus s = openMonday(StoreStatus.ACTIVE).openAt(MONDAY_10_LOCAL,
                List.of(window(AvailabilityType.HOLIDAY), window(AvailabilityType.MAINTENANCE)));
        assertThat(s.open()).isFalse();
        assertThat(s.reasons()).containsExactly(OpenReason.CLOSED_WINDOW);
        assertThat(s.closedWindowTypes()).containsExactly(AvailabilityType.HOLIDAY, AvailabilityType.MAINTENANCE);
    }

    @Test
    @DisplayName("all reasons are collected, in declaration order")
    void allReasons() {
        OpenStatus s = openMonday(StoreStatus.CLOSED).openAt(MONDAY_19_LOCAL, List.of(window(AvailabilityType.EMERGENCY)));
        assertThat(s.orderedReasons()).containsExactly(
                OpenReason.STORE_NOT_ACTIVE, OpenReason.OUTSIDE_HOURS, OpenReason.CLOSED_WINDOW);
    }

    @Test
    @DisplayName("unreadable time zone, missing config or an open day without times → INVALID_SCHEDULE, no exception")
    void invalidSchedule() {
        Store badZone = store(StoreStatus.ACTIVE, "Asia/HoChiMinh",
                List.of(monday(LocalTime.of(8, 0), LocalTime.of(18, 0), false)));
        Store noConfig = store(StoreStatus.ACTIVE, null, List.of(monday(LocalTime.of(8, 0), LocalTime.of(18, 0), false)));
        Store noTimes = store(StoreStatus.ACTIVE, HCM, List.of(monday(null, null, false)));
        Store noHoursList = store(StoreStatus.ACTIVE, HCM, null);

        for (Store s : List.of(badZone, noConfig, noTimes)) {
            assertThatCode(() -> s.openAt(MONDAY_10_LOCAL, List.of())).doesNotThrowAnyException();
            OpenStatus status = s.openAt(MONDAY_10_LOCAL, List.of());
            assertThat(status.open()).isFalse();
            assertThat(status.reasons()).containsExactly(OpenReason.INVALID_SCHEDULE);
        }
        assertThat(noHoursList.openAt(MONDAY_10_LOCAL, List.of()).reasons()).containsExactly(OpenReason.OUTSIDE_HOURS);
        assertThat(badZone.localDate(MONDAY_10_LOCAL)).isEmpty();
    }

    @Test
    @DisplayName("overnight hours (closing before opening) are not supported → OUTSIDE_HOURS (current behaviour)")
    void overnightNotSupported() {
        Store overnight = store(StoreStatus.ACTIVE, HCM, List.of(monday(LocalTime.of(20, 0), LocalTime.of(2, 0), false)));
        Instant monday23Local = Instant.parse("2026-11-16T16:00:00Z");
        assertThat(overnight.openAt(monday23Local, List.of()).reasons()).containsExactly(OpenReason.OUTSIDE_HOURS);
    }

    @Test
    @DisplayName("localDate is the store's date: 17:30Z on the 16th is the 17th in Ho Chi Minh City")
    void localDate() {
        Instant at = Instant.parse("2026-11-16T17:30:00Z");
        assertThat(openMonday(StoreStatus.ACTIVE).localDate(at)).contains(LocalDate.of(2026, 11, 17));
    }
}
