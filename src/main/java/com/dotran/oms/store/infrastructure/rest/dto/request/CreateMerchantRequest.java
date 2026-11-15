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
public class CreateMerchantRequest {

    /** 1-50 ASCII letters or digits (BR-MER-04); unique on the platform, case-insensitive (stored trimmed, upper-case). */
    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;
}
