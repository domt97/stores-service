package com.dotran.example.store.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DomainPageRequest {

    private int pageNumber;
    private int pageSize;
    private String sortBy;
    private String direction;
}
