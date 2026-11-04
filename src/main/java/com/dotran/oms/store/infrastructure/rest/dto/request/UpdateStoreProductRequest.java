package com.dotran.oms.store.infrastructure.rest.dto.request;

import com.dotran.oms.store.domain.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

/**
 * Full replace of a product. Omitted optional fields (description, categoryId, thumbnailUrl) are cleared; SKUs left
 * out are removed; {@code images} replaces the current images ([] removes them all).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreProductRequest {

    /** The version from the last GET of this product. */
    @NotNull
    @Min(0)
    private Long version;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    @Size(max = 255)
    private String thumbnailUrl;

    private UUID categoryId;

    @NotNull
    private ProductStatus status;

    @NotEmpty
    @Size(max = 100)
    private List<@NotNull @Valid UpdateProductSkuRequest> skus;

    @NotNull
    @Size(max = 20)
    private List<@NotNull @Valid UpdateProductImageRequest> images;
}
