package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreProductRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    private UUID categoryId;

    private UUID brandId;

    @NotEmpty
    @Size(max = 100)
    private List<@NotNull @Valid CreateProductSkuRequest> skus;

    @Size(max = 20)
    private List<@NotNull @Valid CreateProductImageRequest> images;
}
