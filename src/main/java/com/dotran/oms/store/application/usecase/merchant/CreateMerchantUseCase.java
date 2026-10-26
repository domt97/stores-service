package com.dotran.oms.store.application.usecase.merchant;

import com.dotran.oms.store.application.command.merchant.CreateMerchantCmd;
import com.dotran.oms.store.application.dto.MerchantDto;

public interface CreateMerchantUseCase {

    MerchantDto create(CreateMerchantCmd cmd);
}
