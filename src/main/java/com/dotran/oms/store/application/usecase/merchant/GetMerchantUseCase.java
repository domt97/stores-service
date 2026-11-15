package com.dotran.oms.store.application.usecase.merchant;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.dto.MerchantDto;

public interface GetMerchantUseCase {

    MerchantDto getById(MerchantId merchantId);
}
