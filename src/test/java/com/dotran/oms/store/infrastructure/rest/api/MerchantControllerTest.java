package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.command.merchant.CreateMerchantCmd;
import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.application.usecase.merchant.CreateMerchantUseCase;
import com.dotran.oms.store.application.usecase.merchant.GetMerchantUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.CreateMerchantRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.MerchantResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.MerchantRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantController")
class MerchantControllerTest {

    @Mock private CreateMerchantUseCase createMerchantUseCase;
    @Mock private GetMerchantUseCase getMerchantUseCase;
    @Mock private MerchantRestMapper merchantRestMapper;

    @InjectMocks
    private MerchantController controller;

    @Test
    @DisplayName("registerMerchant: maps request, calls use case, maps response")
    void registerMerchant_delegates() {
        CreateMerchantRequest request = CreateMerchantRequest.builder().code("M001").name("Tea").build();
        CreateMerchantCmd cmd = mock(CreateMerchantCmd.class);
        MerchantDto dto = mock(MerchantDto.class);
        MerchantResponse response = mock(MerchantResponse.class);
        when(merchantRestMapper.fromRequestToCmd(request)).thenReturn(cmd);
        when(createMerchantUseCase.create(cmd)).thenReturn(dto);
        when(merchantRestMapper.toMerchantResponse(dto)).thenReturn(response);

        assertThat(controller.registerMerchant(request)).isEqualTo(response);
    }

    @Test
    @DisplayName("getMerchant: wraps the path id and returns the mapped response")
    void getMerchant_delegates() {
        UUID merchantId = UUID.randomUUID();
        MerchantDto dto = mock(MerchantDto.class);
        MerchantResponse response = mock(MerchantResponse.class);
        when(getMerchantUseCase.getById(MerchantId.of(merchantId))).thenReturn(dto);
        when(merchantRestMapper.toMerchantResponse(dto)).thenReturn(response);

        assertThat(controller.getMerchant(merchantId)).isEqualTo(response);
    }
}
