package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.example.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.usecase.SettingStoreBusinessHourUseCase;
import com.dotran.example.store.application.usecase.SettingStoreConfigUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.example.store.infrastructure.rest.dto.response.StoreDetailResponse;
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

@WebAdapter
@RestController
@RequestMapping(value = "/v1")
@RequiredArgsConstructor
@Slf4j
public class StoreConfigController {

    private final SettingStoreBusinessHourUseCase settingStoreBusinessHourUseCase;
    private final SettingStoreConfigUseCase settingStoreConfigUseCase;
    private final StoreRestMapper storeRestMapper;

    @PutMapping("/tenants/{tenantId}/stores/{storeId}/business-hours")
    public StoreDetailResponse updateBusinessHour(@PathVariable UUID tenantId,
                                                  @PathVariable UUID storeId,
                                                  @Valid @RequestBody List<UpdateBusinessHourRequest> updateBusinessHourRequests) {
        List<UpdateBusinessHourCmd> updateBusinessHourCmds = storeRestMapper
                .fromListRequestToUpdateBusinessHourCmd(updateBusinessHourRequests);
        StoreDetailDto storeDetailDto = settingStoreBusinessHourUseCase.setupBusinessHour(tenantId, storeId, updateBusinessHourCmds);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @PutMapping("/tenants/{tenantId}/stores/{storeId}/configs")
    public StoreDetailResponse updateBaseConfig(@PathVariable UUID tenantId,
                                                @PathVariable UUID storeId,
                                                @Valid @RequestBody UpdateStoreConfigRequest updateStoreConfigRequest) {
        UpdateStoreConfigCmd cmd = storeRestMapper.fromUpdateStoreConfigToCmd(updateStoreConfigRequest);
        StoreDetailDto storeDetailDto = settingStoreConfigUseCase.setupStoreConfig(tenantId, storeId, cmd);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }
}
