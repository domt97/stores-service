package com.dotran.example.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductSkuRequest {

    @NotBlank
    @Size(max = 100)
    private String sku;

    @Size(max = 255)
    private String name;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @NotBlank @Size(min = 3, max = 3)
    private String currency;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal weight;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal length;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal width;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal height;
}
