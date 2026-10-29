package com.dotran.oms.store.infrastructure.rest.dto.request;

import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductSkuRequest {

    /** Optional; when sent it must be the id of this product's SKU with the same code. */
    private UUID id;

    /** The SKU's identity; cannot change (BR-CAT-08). A new code adds a SKU. */
    @NotBlank
    @Size(max = 100)
    private String sku;

    @Size(max = 255)
    private String name;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 16, fraction = 2)
    private BigDecimal price;

    @NotBlank @Size(min = 3, max = 3)
    private String currency;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal weight;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal length;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal width;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal height;

    @NotNull
    private ProductSkuStatus status;
}
