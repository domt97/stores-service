package com.dotran.example.store.infrastructure.rest;

import com.dotran.example.store.application.command.CreateStoreCmd;
import com.dotran.example.store.application.command.UpdateBusinessHourCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.usecase.SettingStoreConfigUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.StoreConfigRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.UpdateBusinessHourRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.example.store.infrastructure.rest.response.StoreDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    private final SettingStoreConfigUseCase settingStoreConfigUseCase;
    private final StoreRestMapper storeRestMapper;

    @PutMapping("/{tenantId}/{storeId}/business-hour")
    public StoreDetailResponse updateStoreConfig(@PathVariable UUID tenantId,
                                                 @PathVariable UUID storeId,
                                                 @RequestBody List<UpdateBusinessHourRequest> updateBusinessHourRequests) {
        List<UpdateBusinessHourCmd> updateBusinessHourCmds = storeRestMapper.fromListRequestToUpdateBusinessHourCmd(updateBusinessHourRequests);

        StoreDetailDto storeDetailDto = settingStoreConfigUseCase.setupBusinessHour(tenantId, storeId, updateBusinessHourCmds);

        return new StoreDetailResponse(storeDetailDto);
    }
}
