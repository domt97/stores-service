package com.dotran.example.store.infrastructure.rest.response;

import com.dotran.example.store.application.dto.StoreDetailDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StoreDetailResponse extends StoreDetailDto {

    public StoreDetailResponse(StoreDetailDto dto) {
        this.setId(dto.getId());
        this.setTenantId(dto.getTenantId());
        this.setName(dto.getName());
        this.setCode(dto.getCode());
        this.setOwnerId(dto.getOwnerId());
        this.setEmail(dto.getEmail());
        this.setPhone(dto.getPhone());
        this.setStatus(dto.getStatus());
        this.setAddress(dto.getAddress());
        this.setConfig(dto.getConfig());
        this.setBusinessHours(dto.getBusinessHours());
        this.setCreatedAt(dto.getCreatedAt());
        this.setUpdatedAt(dto.getUpdatedAt());
    }

}
