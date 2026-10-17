package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.oms.store.application.command.storeconfig.UpdateStoreConfigCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.usecase.storeconfig.SettingStoreBusinessHourUseCase;
import com.dotran.oms.store.application.usecase.storeconfig.SettingStoreConfigUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.BusinessHourRequest;
import com.dotran.oms.store.infrastructure.rest.dto.request.UpdateStoreConfigRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreDetailResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.StoreRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreConfigController")
class StoreConfigControllerTest {

    @Mock private SettingStoreBusinessHourUseCase settingStoreBusinessHourUseCase;
    @Mock private SettingStoreConfigUseCase settingStoreConfigUseCase;
    @Mock private StoreRestMapper storeRestMapper;

    @InjectMocks
    private StoreConfigController controller;

    @Test
    @DisplayName("updateBusinessHour: maps requests, calls use case, maps response")
    void updateBusinessHour_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        BusinessHourRequest hourRequest = mock(BusinessHourRequest.class);
        UpdateBusinessHourCmd cmd = mock(UpdateBusinessHourCmd.class);
        StoreDetailDto dto = mock(StoreDetailDto.class);
        StoreDetailResponse response = mock(StoreDetailResponse.class);

        when(storeRestMapper.fromListRequestToUpdateBusinessHourCmd(List.of(hourRequest))).thenReturn(List.of(cmd));
        when(settingStoreBusinessHourUseCase.setupBusinessHour(eq(tenantId), eq(storeId), any())).thenReturn(dto);
        when(storeRestMapper.toStoreDetailResponse(dto)).thenReturn(response);

        StoreDetailResponse result = controller.updateBusinessHour(tenantId, storeId, List.of(hourRequest));

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("updateBaseConfig: maps request, calls use case, maps response")
    void updateBaseConfig_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UpdateStoreConfigRequest request = mock(UpdateStoreConfigRequest.class);
        UpdateStoreConfigCmd cmd = mock(UpdateStoreConfigCmd.class);
        StoreDetailDto dto = mock(StoreDetailDto.class);
        StoreDetailResponse response = mock(StoreDetailResponse.class);

        when(storeRestMapper.fromUpdateStoreConfigToCmd(request)).thenReturn(cmd);
        when(settingStoreConfigUseCase.setupStoreConfig(tenantId, storeId, cmd)).thenReturn(dto);
        when(storeRestMapper.toStoreDetailResponse(dto)).thenReturn(response);

        StoreDetailResponse result = controller.updateBaseConfig(tenantId, storeId, request);

        assertThat(result).isEqualTo(response);
    }
}
