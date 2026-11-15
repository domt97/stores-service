package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductImageRequest {

    @NotBlank
    @Size(max = 2048)
    private String imageUrl;

    @NotNull
    @Min(0)
    private Integer displayOrder;
}
