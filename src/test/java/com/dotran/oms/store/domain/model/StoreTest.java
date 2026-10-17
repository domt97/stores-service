package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.exception.StoreAlreadyClosedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Store")
class StoreTest {

    private Store activeStore() {
        return Store.builder()
                .id(StoreId.newStoreId())
                .tenantId(TenantId.newTenantId())
                .name("Test Store")
                .code("TST001")
                .ownerId(CustomerId.newCustomerId())
                .status(StoreStatus.ACTIVE)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Store closedStore() {
        return Store.builder()
                .id(StoreId.newStoreId())
                .tenantId(TenantId.newTenantId())
                .name("Test Store")
                .code("TST001")
                .ownerId(CustomerId.newCustomerId())
                .status(StoreStatus.CLOSED)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("transitions status to CLOSED when store is ACTIVE")
        void closesActiveStore() {
            Store store = activeStore();
            store.close();
            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
        }

        @Test
        @DisplayName("throws StoreAlreadyClosedException when store is already CLOSED")
        void throwsException_whenAlreadyClosed() {
            Store store = closedStore();
            assertThatThrownBy(store::close)
                    .isInstanceOf(StoreAlreadyClosedException.class);
        }

        @Test
        @DisplayName("updates updatedAt when closing")
        void updatesTimestamp_whenClosing() {
            Store store = activeStore();
            Instant before = store.getUpdatedAt();
            store.close();
            assertThat(store.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("reopen")
    class Reopen {

        @Test
        @DisplayName("transitions status to ACTIVE when store is CLOSED")
        void reopensClosedStore() {
            Store store = closedStore();
            store.reopen();
            assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws BusinessException when store is not CLOSED")
        void throwsException_whenNotClosed() {
            Store store = activeStore();
            assertThatThrownBy(store::reopen)
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("updates updatedAt when reopening")
        void updatesTimestamp_whenReopening() {
            Store store = closedStore();
            Instant before = store.getUpdatedAt();
            store.reopen();
            assertThat(store.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("updateBusinessHour")
    class UpdateBusinessHour {

        @Test
        @DisplayName("updates only the matching day, leaving all other days unchanged")
        void updatesOnlyMatchingDay() {
            Store store = activeStore();
            store.addBusinessHour(BusinessHour.defaultConfig());

            LocalTime newOpen = LocalTime.of(10, 0);
            LocalTime newClose = LocalTime.of(22, 0);
            store.updateBusinessHour(DayOfWeek.MONDAY, newOpen, newClose, true);

            BusinessHour monday = store.getBusinessHours().stream()
                    .filter(bh -> bh.getDayOfWeek() == DayOfWeek.MONDAY)
                    .findFirst().orElseThrow();

            BusinessHour tuesday = store.getBusinessHours().stream()
                    .filter(bh -> bh.getDayOfWeek() == DayOfWeek.TUESDAY)
                    .findFirst().orElseThrow();

            assertThat(monday.getOpeningTime()).isEqualTo(newOpen);
            assertThat(monday.getClosingTime()).isEqualTo(newClose);
            assertThat(monday.isClosed()).isTrue();
            assertThat(tuesday.getOpeningTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(tuesday.getClosingTime()).isEqualTo(LocalTime.of(18, 0));
        }

        @Test
        @DisplayName("updates updatedAt after changing a business hour")
        void updatesTimestamp_afterChange() {
            Store store = activeStore();
            store.addBusinessHour(BusinessHour.defaultConfig());
            Instant before = store.getUpdatedAt();
            store.updateBusinessHour(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(20, 0), false);
            assertThat(store.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("initStore")
    class InitStore {

        @Test
        @DisplayName("creates a store with ACTIVE status")
        void createsActiveStore() {
            TenantId tenantId = TenantId.newTenantId();
            Store store = Store.initStore(tenantId, "My Store", "STR001",
                    CustomerId.newCustomerId(), "a@b.com", "0123456789");

            assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
            assertThat(store.getTenantId()).isEqualTo(tenantId);
            assertThat(store.getId()).isNotNull();
        }

        @Test
        @DisplayName("initializes with an empty business hours list")
        void initializesWithEmptyBusinessHours() {
            Store store = Store.initStore(TenantId.newTenantId(), "My Store", "STR001",
                    CustomerId.newCustomerId(), "a@b.com", "0123456789");

            assertThat(store.getBusinessHours()).isNotNull().isEmpty();
        }
    }
}
