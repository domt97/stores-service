package com.dotran.example.store.domain.model;

import com.dotran.example.store.common.domain.AggregateRoot;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.valueobject.TenantSetting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class TenantInfo extends AggregateRoot<TenantId> {

    private String code;

    private String name;

    private String status;

    private TenantSetting settings;

    private Instant createdAt;

    private Instant updatedAt;

}
