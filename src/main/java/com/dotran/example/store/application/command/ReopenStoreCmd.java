package com.dotran.example.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReopenStoreCmd {

    private UUID tenantId;

    private UUID storeId;
}
