package com.dotran.oms.store.application.service.merchant;

import com.dotran.oms.store.application.command.merchant.CreateMerchantCmd;
import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.application.mapper.MerchantDataMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import com.dotran.oms.store.domain.exception.MerchantCodeAlreadyExistsException;
import com.dotran.oms.store.domain.model.Merchant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMerchantService")
class CreateMerchantServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantDataMapper merchantDataMapper;

    @InjectMocks
    private CreateMerchantService createMerchantService;

    @Test
    @DisplayName("registers an ACTIVE merchant with a normalized code and returns the saved one")
    void create_registersMerchant() {
        CreateMerchantCmd cmd = CreateMerchantCmd.builder().code(" m001 ").name("Tea Chain").build();
        Merchant saved = mock(Merchant.class);
        MerchantDto dto = mock(MerchantDto.class);

        when(merchantRepository.existsByCode("M001")).thenReturn(false);
        when(merchantRepository.create(any(Merchant.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(com.dotran.oms.core.domain.id.MerchantId.newMerchantId());
        when(merchantDataMapper.toMerchantDto(saved)).thenReturn(dto);

        MerchantDto result = createMerchantService.create(cmd);

        assertThat(result).isEqualTo(dto);
        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).create(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("M001");
        assertThat(captor.getValue().getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        // the response is built from the persisted merchant, not from the in-memory one
        verify(merchantDataMapper).toMerchantDto(saved);
    }

    @Test
    @DisplayName("duplicate code (any case) → MerchantCodeAlreadyExistsException, nothing saved")
    void create_rejectsDuplicateCode() {
        CreateMerchantCmd cmd = CreateMerchantCmd.builder().code("m001").name("Tea Chain").build();
        when(merchantRepository.existsByCode("M001")).thenReturn(true);

        assertThatThrownBy(() -> createMerchantService.create(cmd))
                .isInstanceOf(MerchantCodeAlreadyExistsException.class);
        verify(merchantRepository, never()).create(any());
    }
}
