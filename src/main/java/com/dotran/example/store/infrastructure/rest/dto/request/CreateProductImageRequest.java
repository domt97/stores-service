package com.dotran.example.store.infrastructure.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductImageRequest {

    @NotBlank
    private String imageUrl;

    @NotNull
    private Integer displayOrder;
}
