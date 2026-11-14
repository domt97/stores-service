package com.dotran.oms.store.application.service.merchant;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.application.mapper.MerchantDataMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.domain.model.Merchant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMerchantService")
class GetMerchantServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantDataMapper merchantDataMapper;

    @InjectMocks
    private GetMerchantService getMerchantService;

    @Test
    @DisplayName("returns the merchant DTO when found")
    void getById_returnsDto() {
        MerchantId merchantId = MerchantId.newMerchantId();
        Merchant merchant = mock(Merchant.class);
        MerchantDto dto = mock(MerchantDto.class);
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantDataMapper.toMerchantDto(merchant)).thenReturn(dto);

        assertThat(getMerchantService.getById(merchantId)).isEqualTo(dto);
    }

    @Test
    @DisplayName("missing merchant → NotFoundException")
    void getById_throwsWhenMissing() {
        MerchantId merchantId = MerchantId.newMerchantId();
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMerchantService.getById(merchantId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Merchant not found");
        verifyNoInteractions(merchantDataMapper);
    }
}
