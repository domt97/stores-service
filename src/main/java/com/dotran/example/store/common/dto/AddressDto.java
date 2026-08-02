package com.dotran.example.store.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressDto {

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String ward;
    private String district;
    private String province;
    private String city;
    private String country;
    private String postalCode;
}
