package com.dotran.oms.store.domain.model;

import java.util.Optional;
import java.util.EnumSet;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.DateTimeException;
import com.dotran.oms.store.domain.valueobject.OpenStatus;
import com.dotran.oms.store.domain.enums.OpenReason;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.valueobject.Address;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.exception.StoreAlreadyClosedException;
import com.dotran.oms.store.domain.valueobject.StoreConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.dotran.oms.store.common.constants.Constants.ERROR_MSG_STORE_IS_NOT_CLOSED;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Store extends AggregateRoot<StoreId> {

    private MerchantId merchantId;
    private String name;
    private String code;
    private CustomerId ownerId;
    private String email;
    private String phone;
    private StoreStatus status;

    private Address address;
    private StoreConfig config;
    private List<BusinessHour> businessHours;

    private Instant createdAt;
    private Instant updatedAt;

    public static Store initStore(MerchantId merchantId,
                                  String name,
                                  String code,
                                  CustomerId ownerId,
                                  String email,
                                  String phone) {
        return Store.builder()
                .id(StoreId.newStoreId())
                .merchantId(merchantId)
                .name(name)
                .code(code)
                .ownerId(ownerId)
                .email(email)
                .phone(phone)
                .status(StoreStatus.ACTIVE)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void addAddress(Address address) {
        this.address = address;
    }

    public void addConfig(StoreConfig config) {
        this.config = config;
    }

    public void addBusinessHour(List<BusinessHour> businessHours) {
        this.businessHours = businessHours;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void activate() {
        this.status = StoreStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = StoreStatus.INACTIVE;
    }

    public void close() {
        if (status == StoreStatus.CLOSED) {
            throw new StoreAlreadyClosedException();
        }
        this.updatedAt = Instant.now();
        this.status = StoreStatus.CLOSED;
    }

    public void reopen() {
        if (status != StoreStatus.CLOSED) {
            throw new BusinessException(ERROR_MSG_STORE_IS_NOT_CLOSED);
        }
        this.updatedAt = Instant.now();
        this.status = StoreStatus.ACTIVE;
    }

    public void updateConfig(Boolean autoAcceptOrder,
                             Boolean allowPreOrder,
                             LocalTime openingTime,
                             LocalTime closingTime,
                             String timeZone,
                             String currency,
                             Integer preparationTimeMinutes,
                             Integer maxOrdersPerDay) {
        this.config = this.config.withUpdated(
                autoAcceptOrder,
                allowPreOrder,
                openingTime,
                closingTime,
                timeZone,
                currency,
                preparationTimeMinutes,
                maxOrdersPerDay);
        this.updatedAt = Instant.now();
    }

    public void updateBusinessHour(DayOfWeek dayOfWeek, LocalTime openingTime, LocalTime closingTime, boolean closed) {
        this.businessHours = this.businessHours.stream()
                .map(bh -> bh.getDayOfWeek() == dayOfWeek
                        ? bh.withUpdated(openingTime, closingTime, closed)
                        : bh)
                .toList();
        this.updatedAt = Instant.now();
    }

    public boolean isOpen(Instant now) {
        return config.isOpen(now, businessHours);
    }

    /**
     * Whether the store takes orders at {@code now} (BR-STO-02, BR-STO-04): ACTIVE, inside today's business hours in its
     * time zone, and not covered by an availability window. {@code coveringWindows} are the windows already selected as
     * covering {@code now} (not cancelled, started, not ended); each one is reported. Every reason is collected. A time
     * zone or a business day that cannot be read gives {@link OpenReason#INVALID_SCHEDULE} (closed), never an exception.
     */
    public OpenStatus openAt(Instant now, List<StoreAvailability> coveringWindows) {
        EnumSet<OpenReason> reasons = EnumSet.noneOf(OpenReason.class);
        if (status != StoreStatus.ACTIVE) {
            reasons.add(OpenReason.STORE_NOT_ACTIVE);
        }
        Optional<ZoneId> zone = zone();
        if (zone.isEmpty()) {
            reasons.add(OpenReason.INVALID_SCHEDULE);
        } else {
            ZonedDateTime local = now.atZone(zone.get());
            BusinessHour today = businessHours == null ? null : businessHours.stream()
                    .filter(h -> h.getDayOfWeek() == local.getDayOfWeek())
                    .findFirst().orElse(null);
            if (today == null || today.isClosed()) {
                reasons.add(OpenReason.OUTSIDE_HOURS);
            } else if (today.getOpeningTime() == null || today.getClosingTime() == null) {
                reasons.add(OpenReason.INVALID_SCHEDULE);
            } else if (!today.isOpen(local.toLocalTime())) {
                reasons.add(OpenReason.OUTSIDE_HOURS);
            }
        }
        List<AvailabilityType> windowTypes = coveringWindows.stream().map(StoreAvailability::getType).toList();
        if (!windowTypes.isEmpty()) {
            reasons.add(OpenReason.CLOSED_WINDOW);
        }
        return new OpenStatus(reasons.isEmpty(), reasons, windowTypes);
    }

    /** The store's local date at {@code now}, or empty when its time zone cannot be read. */
    public Optional<LocalDate> localDate(Instant now) {
        return zone().map(z -> now.atZone(z).toLocalDate());
    }

    private Optional<ZoneId> zone() {
        if (config == null || config.getTimeZone() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(config.getTimeZone()));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }
}
