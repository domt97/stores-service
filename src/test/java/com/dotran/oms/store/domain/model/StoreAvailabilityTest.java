package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoreAvailability")
class StoreAvailabilityTest {

    private final Instant now = Instant.now();

    private StoreAvailability buildAvailability(boolean cancelled) {
        return StoreAvailability.builder()
                .id(StoreAvailabilityId.generateId())
                .storeId(StoreId.newStoreId())
                .type(AvailabilityType.HOLIDAY)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now.plus(1, ChronoUnit.HOURS))
                .reason("Public Holiday")
                .cancelled(cancelled)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Nested
    @DisplayName("newStoreAvailability")
    class NewStoreAvailability {

        @Test
        @DisplayName("generates a new ID")
        void generatesNewId() {
            StoreAvailability availability = StoreAvailability.builder()
                    .storeId(StoreId.newStoreId())
                    .type(AvailabilityType.MAINTENANCE)
                    .startTime(now)
                    .build();
            availability.newStoreAvailability();
            assertThat(availability.getId()).isNotNull();
        }

        @Test
        @DisplayName("sets cancelled to false")
        void setsCancelledFalse() {
            StoreAvailability availability = StoreAvailability.builder()
                    .storeId(StoreId.newStoreId())
                    .type(AvailabilityType.HOLIDAY)
                    .startTime(now)
                    .build();
            availability.newStoreAvailability();
            assertThat(availability.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("sets createdAt and updatedAt")
        void setsTimestamps() {
            StoreAvailability availability = StoreAvailability.builder()
                    .storeId(StoreId.newStoreId())
                    .type(AvailabilityType.HOLIDAY)
                    .startTime(now)
                    .build();
            availability.newStoreAvailability();
            assertThat(availability.getCreatedAt()).isNotNull();
            assertThat(availability.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("returns false when availability is cancelled")
        void returnsFalse_whenCancelled() {
            StoreAvailability availability = buildAvailability(true);
            assertThat(availability.isActive(now)).isFalse();
        }

        @Test
        @DisplayName("returns false when current time is before startTime")
        void returnsFalse_whenBeforeStart() {
            StoreAvailability availability = StoreAvailability.builder()
                    .id(StoreAvailabilityId.generateId())
                    .storeId(StoreId.newStoreId())
                    .type(AvailabilityType.HOLIDAY)
                    .startTime(now.plus(1, ChronoUnit.HOURS))
                    .endTime(now.plus(2, ChronoUnit.HOURS))
                    .cancelled(false)
                    .build();
            assertThat(availability.isActive(now)).isFalse();
        }

        @Test
        @DisplayName("returns false when current time is after endTime")
        void returnsFalse_whenAfterEnd() {
            StoreAvailability availability = StoreAvailability.builder()
                    .id(StoreAvailabilityId.generateId())
                    .storeId(StoreId.newStoreId())
                    .type(AvailabilityType.HOLIDAY)
                    .startTime(now.minus(2, ChronoUnit.HOURS))
                    .endTime(now.minus(1, ChronoUnit.HOURS))
                    .cancelled(false)
                    .build();
            assertThat(availability.isActive(now)).isFalse();
        }

        @Test
        @DisplayName("returns true when current time is within the active window")
        void returnsTrue_whenWithinWindow() {
            StoreAvailability availability = buildAvailability(false);
            assertThat(availability.isActive(now)).isTrue();
        }

        @Test
        @DisplayName("returns true when endTime is null (open-ended availability)")
        void returnsTrue_whenEndTimeIsNull() {
            StoreAvailability availability = StoreAvailability.builder()
                    .id(StoreAvailabilityId.generateId())
                    .storeId(StoreId.newStoreId())
                    .type(AvailabilityType.HOLIDAY)
                    .startTime(now.minus(1, ChronoUnit.HOURS))
                    .endTime(null)
                    .cancelled(false)
                    .build();
            assertThat(availability.isActive(now)).isTrue();
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("sets cancelled to true")
        void setsCancelledTrue() {
            StoreAvailability availability = buildAvailability(false);
            availability.cancel();
            assertThat(availability.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("updates updatedAt when cancelled")
        void updatesTimestamp_whenCancelled() {
            StoreAvailability availability = buildAvailability(false);
            Instant before = availability.getUpdatedAt();
            availability.cancel();
            assertThat(availability.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("throws BusinessException when already cancelled")
        void throwsException_whenAlreadyCancelled() {
            StoreAvailability availability = buildAvailability(true);
            assertThatThrownBy(availability::cancel)
                    .isInstanceOf(BusinessException.class);
        }
    }
}
