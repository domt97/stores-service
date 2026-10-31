package com.dotran.oms.store.infrastructure.rest.dto.response;

import com.dotran.oms.store.domain.enums.MerchantStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MerchantResponse {

    private UUID id;
    private String code;
    private String name;
    private MerchantStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
