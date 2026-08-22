package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.example.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.usecase.storeconfig.SettingStoreBusinessHourUseCase;
import com.dotran.example.store.application.usecase.storeconfig.SettingStoreConfigUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Store Configuration", description = "APIs for configuring store settings including business hours and operational configurations")
@WebAdapter
@RestController
@RequestMapping(value = "/v1")
@RequiredArgsConstructor
@Slf4j
public class StoreConfigController {

    private final SettingStoreBusinessHourUseCase settingStoreBusinessHourUseCase;
    private final SettingStoreConfigUseCase settingStoreConfigUseCase;
    private final StoreRestMapper storeRestMapper;

    @Operation(summary = "Update business hours", description = "Updates the business hours configuration for a store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Business hours updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @PutMapping("/tenants/{tenantId}/stores/{storeId}/business-hours")
    public StoreDetailResponse updateBusinessHour(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Valid @RequestBody List<UpdateBusinessHourRequest> updateBusinessHourRequests) {
        List<UpdateBusinessHourCmd> updateBusinessHourCmds = storeRestMapper
                .fromListRequestToUpdateBusinessHourCmd(updateBusinessHourRequests);
        StoreDetailDto storeDetailDto = settingStoreBusinessHourUseCase.setupBusinessHour(tenantId, storeId, updateBusinessHourCmds);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @Operation(summary = "Update store configuration", description = "Updates the operational configuration settings for a store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store configuration updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @PutMapping("/tenants/{tenantId}/stores/{storeId}/configs")
    public StoreDetailResponse updateBaseConfig(
            @Parameter(description = "Tenant ID", required = true) @PathVariable UUID tenantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreConfigRequest updateStoreConfigRequest) {
        UpdateStoreConfigCmd cmd = storeRestMapper.fromUpdateStoreConfigToCmd(updateStoreConfigRequest);
        StoreDetailDto storeDetailDto = settingStoreConfigUseCase.setupStoreConfig(tenantId, storeId, cmd);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }
}
