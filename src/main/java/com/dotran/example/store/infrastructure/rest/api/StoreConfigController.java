package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.UpdateBusinessHourCmd;
import com.dotran.example.store.application.command.UpdateStoreConfigCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.usecase.SettingStoreBusinessHourUseCase;
import com.dotran.example.store.application.usecase.SettingStoreConfigUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.example.store.infrastructure.rest.response.StoreDetailResponse;
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
@RequestMapping(value = "/v1/store/configs")
@RequiredArgsConstructor
@Slf4j
public class StoreConfigController {

    private final SettingStoreBusinessHourUseCase settingStoreBusinessHourUseCase;
    private final SettingStoreConfigUseCase settingStoreConfigUseCase;
    private final StoreRestMapper storeRestMapper;

    @PutMapping("/{tenantId}/{storeId}/business-hour")
    public StoreDetailResponse updateBusinessHour(@PathVariable UUID tenantId,
                                                  @PathVariable UUID storeId,
                                                  @Valid @RequestBody List<UpdateBusinessHourRequest> updateBusinessHourRequests) {
        List<UpdateBusinessHourCmd> updateBusinessHourCmds = storeRestMapper
                .fromListRequestToUpdateBusinessHourCmd(updateBusinessHourRequests);
        StoreDetailDto storeDetailDto = settingStoreBusinessHourUseCase.setupBusinessHour(tenantId, storeId, updateBusinessHourCmds);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @PutMapping("/{tenantId}/{storeId}")
    public StoreDetailResponse updateBaseConfig(@PathVariable UUID tenantId,
                                                @PathVariable UUID storeId,
                                                @Valid @RequestBody UpdateStoreConfigRequest updateStoreConfigRequest) {
        UpdateStoreConfigCmd cmd = storeRestMapper.fromUpdateStoreConfigToCmd(updateStoreConfigRequest);
        StoreDetailDto storeDetailDto = settingStoreConfigUseCase.setupStoreConfig(tenantId, storeId, cmd);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }
}
