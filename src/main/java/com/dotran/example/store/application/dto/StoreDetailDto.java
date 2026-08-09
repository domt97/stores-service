package com.dotran.example.store.application.dto;

import com.dotran.example.store.common.dto.AddressDto;
import com.dotran.example.store.domain.enums.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailDto {

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
