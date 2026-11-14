package com.dotran.oms.store.application.dto;

import com.dotran.oms.store.domain.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDto {

    private UUID id;
    private String code;
    private String name;
    private MerchantStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
