package com.dotran.example.store.domain.valueobject;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class TenantSetting {

    private String timeZone;

    private String currency;

    private String locale;
}
