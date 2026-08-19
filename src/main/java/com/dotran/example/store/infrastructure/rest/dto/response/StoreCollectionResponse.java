package com.dotran.example.store.infrastructure.rest.dto.response;

import com.dotran.example.store.domain.enums.CollectionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StoreCollectionResponse {

    private UUID id;
    private String name;
    private String description;
    private Integer productCount;
    private CollectionStatus status;
}
