package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.oms.store.application.command.store.CreateStoreCmd;
import com.dotran.oms.store.application.dto.StoreAvailabilityDto;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.usecase.store.AddStoreAvailabilityUseCase;
import com.dotran.oms.store.application.usecase.store.CancelStoreAvailabilityUseCase;
import com.dotran.oms.store.application.usecase.store.CloseStoreUseCase;
import com.dotran.oms.store.application.usecase.store.CreateStoreUseCase;
import com.dotran.oms.store.application.usecase.store.GetStoreUseCase;
import com.dotran.oms.store.application.usecase.store.ReopenStoreUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.AddStoreAvailabilityRequest;
import com.dotran.oms.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreAvailabilityResponse;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreDetailResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.StoreRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreController")
class StoreControllerTest {

    @Mock private StoreRestMapper storeRestMapper;
    @Mock private CreateStoreUseCase createStoreUseCase;
    @Mock private GetStoreUseCase getStoreUseCase;
    @Mock private CloseStoreUseCase closeStoreUseCase;
    @Mock private ReopenStoreUseCase reopenStoreUseCase;
    @Mock private AddStoreAvailabilityUseCase addStoreAvailabilityUseCase;
    @Mock private CancelStoreAvailabilityUseCase cancelStoreAvailabilityUseCase;

    @InjectMocks
    private StoreController controller;

    @Test
    @DisplayName("createStore: maps request, calls use case, maps response")
    void createStore_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        CreateStoreRequest request = mock(CreateStoreRequest.class);
        CreateStoreCmd cmd = mock(CreateStoreCmd.class);
        StoreDetailDto dto = mock(StoreDetailDto.class);
        StoreDetailResponse response = mock(StoreDetailResponse.class);

        when(storeRestMapper.fromRequestToCmd(request)).thenReturn(cmd);
        when(createStoreUseCase.create(cmd)).thenReturn(dto);
        when(storeRestMapper.toStoreDetailResponse(dto)).thenReturn(response);

        StoreDetailResponse result = controller.createStore(tenantId, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("getStore: builds command, calls use case, maps response")
    void getStore_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        StoreDetailDto dto = mock(StoreDetailDto.class);
        StoreDetailResponse response = mock(StoreDetailResponse.class);

        when(getStoreUseCase.getStoreByTenantIdAndStoreId(any())).thenReturn(dto);
        when(storeRestMapper.toStoreDetailResponse(dto)).thenReturn(response);

        StoreDetailResponse result = controller.getStore(tenantId, storeId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("closeStore: maps command, calls use case, maps response")
    void closeStore_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        StoreDetailDto dto = mock(StoreDetailDto.class);
        StoreDetailResponse response = mock(StoreDetailResponse.class);

        when(storeRestMapper.fromRequestToCloseStoreCmd(tenantId, storeId)).thenReturn(mock(com.dotran.oms.store.application.command.store.CloseStoreCmd.class));
        when(closeStoreUseCase.close(any())).thenReturn(dto);
        when(storeRestMapper.toStoreDetailResponse(dto)).thenReturn(response);

        StoreDetailResponse result = controller.closeStore(tenantId, storeId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("reopenStore: builds command, calls use case, maps response")
    void reopenStore_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        StoreDetailDto dto = mock(StoreDetailDto.class);
        StoreDetailResponse response = mock(StoreDetailResponse.class);

        when(reopenStoreUseCase.reopen(any())).thenReturn(dto);
        when(storeRestMapper.toStoreDetailResponse(dto)).thenReturn(response);

        StoreDetailResponse result = controller.reopenStore(tenantId, storeId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("addStoreAvailability: maps request, enriches cmd, calls use case, maps response")
    void addStoreAvailability_delegatesAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        AddStoreAvailabilityRequest request = mock(AddStoreAvailabilityRequest.class);
        AddStoreAvailabilityCmd cmd = new AddStoreAvailabilityCmd();
        StoreAvailabilityDto dto = mock(StoreAvailabilityDto.class);
        StoreAvailabilityResponse response = mock(StoreAvailabilityResponse.class);

        when(storeRestMapper.fromRequestToAddStoreAvailabilityCmd(request)).thenReturn(cmd);
        when(addStoreAvailabilityUseCase.add(cmd)).thenReturn(dto);
        when(storeRestMapper.toStoreAvailabilityResponse(dto)).thenReturn(response);

        StoreAvailabilityResponse result = controller.addStoreAvailability(tenantId, storeId, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("cancelStoreAvailability: delegates to use case with correct ids")
    void cancelStoreAvailability_delegatesToUseCase() {
        UUID tenantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID availabilityId = UUID.randomUUID();

        controller.cancelStoreAvailability(tenantId, storeId, availabilityId);

        verify(cancelStoreAvailabilityUseCase).cancel(availabilityId, storeId);
    }
}
