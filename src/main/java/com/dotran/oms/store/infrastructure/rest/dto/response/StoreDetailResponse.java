package com.dotran.oms.store.infrastructure.rest.dto.response;

import com.dotran.oms.store.common.dto.AddressDto;
import com.dotran.oms.store.application.dto.BusinessHourDto;
import com.dotran.oms.store.application.dto.ConfigDto;
import com.dotran.oms.store.domain.enums.StoreStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class StoreDetailResponse {

    private UUID id;
    private String tenantId;
    private String name;
    private String code;
    private String ownerId;
    private String email;
    private String phone;
    private StoreStatus status;
    private AddressDto address;
    private ConfigDto config;
    private List<BusinessHourDto> businessHours;
    private Instant createdAt;
    private Instant updatedAt;
}
