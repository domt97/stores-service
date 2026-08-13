package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.AggregateRoot;
import com.dotran.example.store.common.domain.valueobject.Address;
import com.dotran.example.store.common.domain.valueobject.CustomerId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.enums.StoreStatus;
import com.dotran.example.store.domain.exception.BusinessException;
import com.dotran.example.store.domain.exception.StoreAlreadyClosedException;
import com.dotran.example.store.domain.valueobject.StoreConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.dotran.example.store.common.constants.Constants.ERROR_MSG_STORE_IS_NOT_CLOSED;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Store extends AggregateRoot<StoreId> {

    private TenantId tenantId;
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

    public static Store initStore(TenantId tenantId,
                                  String name,
                                  String code,
                                  CustomerId ownerId,
                                  String email,
                                  String phone) {
        return Store.builder()
                .id(StoreId.newStoreId())
                .tenantId(tenantId)
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
        this.config.updateConfig(autoAcceptOrder, allowPreOrder, openingTime, closingTime, timeZone, currency, preparationTimeMinutes, maxOrdersPerDay);
        this.updatedAt = Instant.now();
    }

    public boolean isOpen(Instant now) {
        return config.isOpen(now, businessHours);
    }
}
