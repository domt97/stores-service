package com.dotran.example.store.infrastructure.rest.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStoreProductsRequest {

    private String path;
    private String fileName;
}
