package com.dotran.oms.store.application.service.merchant;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.application.mapper.MerchantDataMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.usecase.merchant.GetMerchantUseCase;
import com.dotran.oms.store.common.constants.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetMerchantService implements GetMerchantUseCase {

    private final MerchantRepository merchantRepository;
    private final MerchantDataMapper merchantDataMapper;

    @Override
    @Transactional(readOnly = true)
    public MerchantDto getById(MerchantId merchantId) {
        return merchantRepository.findById(merchantId)
                .map(merchantDataMapper::toMerchantDto)
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND));
    }
}
