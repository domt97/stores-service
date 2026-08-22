package com.dotran.example.store.application.command.storeproduct;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageCmd {

    private String imageUrl;

    private Integer displayOrder;
}