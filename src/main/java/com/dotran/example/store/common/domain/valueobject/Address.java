package com.dotran.example.store.common.domain.valueobject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {

    private final String phone;
    private final String addressLine1;
    private final String addressLine2;
    private final String ward;
    private final String district;
    private final String province;
    private final String city;
    private final String country;
    private final String postalCode;

    public Address init(String phone,
                        String addressLine1,
                        String addressLine2,
                        String ward,
                        String district,
                        String province,
                        String city,
                        String country,
                        String postalCode) {
        return Address.builder()
                .phone(phone)
                .addressLine1(addressLine1)
                .addressLine2(addressLine2)
                .ward(ward)
                .district(district)
                .province(province)
                .city(city)
                .country(country)
                .postalCode(postalCode)
                .build();
    }

}
