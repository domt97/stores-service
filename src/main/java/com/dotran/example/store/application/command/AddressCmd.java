package com.dotran.example.store.application.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressCmd {

    private String recipientName;
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
