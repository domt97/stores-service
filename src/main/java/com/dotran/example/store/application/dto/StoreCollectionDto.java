package com.dotran.example.store.application.dto;

import com.dotran.example.store.domain.enums.CollectionStatus;
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
public class StoreCollectionDto {

    private UUID id;
    private UUID storeId;
    private String name;
    private String description;
    private CollectionStatus status;
    private Integer productCount;
    private Instant createdAt;
    private Instant updatedAt;
}
