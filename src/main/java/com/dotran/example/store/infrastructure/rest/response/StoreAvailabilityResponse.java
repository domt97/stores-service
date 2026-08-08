package com.dotran.example.store.infrastructure.rest.response;

import com.dotran.example.store.application.dto.StoreAvailabilityDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class StoreAvailabilityResponse extends StoreAvailabilityDto {

    public StoreAvailabilityResponse(StoreAvailabilityDto dto) {
        this.setId(dto.getId());
        this.setStoreId(dto.getStoreId());
        this.setType(dto.getType());
        this.setStartTime(dto.getStartTime());
        this.setEndTime(dto.getEndTime());
        this.setReason(dto.getReason());
        this.setCancelled(dto.isCancelled());
        this.setCreatedAt(dto.getCreatedAt());
        this.setUpdatedAt(dto.getUpdatedAt());
    }
}
