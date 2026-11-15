package com.dotran.oms.store.application.service.merchant;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.store.application.command.merchant.CreateMerchantCmd;
import com.dotran.oms.store.application.dto.MerchantDto;
import com.dotran.oms.store.application.mapper.MerchantDataMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.usecase.merchant.CreateMerchantUseCase;
import com.dotran.oms.store.domain.exception.MerchantCodeAlreadyExistsException;
import com.dotran.oms.store.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seller self-registration (BR-MER-06): the merchant is ACTIVE immediately (BR-MER-05).
 * The registering user becomes OWNER once identity exists (open question Q36).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateMerchantService implements CreateMerchantUseCase {

    private final MerchantRepository merchantRepository;
    private final MerchantDataMapper merchantDataMapper;

    @Override
    @Transactional
    public MerchantDto create(CreateMerchantCmd cmd) {
        Merchant merchant = Merchant.register(cmd.getCode(), cmd.getName());

        if (merchantRepository.existsByCode(merchant.getCode())) {
            throw new MerchantCodeAlreadyExistsException();
        }

        Merchant created = merchantRepository.create(merchant);
        log.info("Merchant registered: id = {}, code = {}", created.getId().getValue(), created.getCode());

        return merchantDataMapper.toMerchantDto(created);
    }
}
