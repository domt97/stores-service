package com.dotran.example.store.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {

    private Long id;
    private UUID productId;

    private String imageUrl;
    private Integer displayOrder;

    private Instant createdAt;
}
