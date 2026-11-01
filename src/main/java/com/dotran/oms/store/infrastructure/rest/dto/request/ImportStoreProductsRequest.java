package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStoreProductsRequest {

    /** S3 bucket holding the file (open question 25: taken from the request). */
    @NotBlank
    private String path;

    /** S3 key of the .xlsx file. */
    @NotBlank
    @Size(max = 1024)
    private String fileName;
}
