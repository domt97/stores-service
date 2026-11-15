package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.oms.store.application.dto.StoreAvailabilityDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.store.AddStoreAvailabilityUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.StoreAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds an availability window (closure) to a store. The store must belong to the path merchant (system rule 4);
 * otherwise 404 and nothing is written.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class AddStoreAvailabilityService implements AddStoreAvailabilityUseCase {

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreAvailabilityRepository storeAvailabilityRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreAvailabilityDto add(AddStoreAvailabilityCmd cmd) {
        log.info("AddStoreAvailabilityService - add: START for storeId={}", cmd.getStoreId());
        if (!merchantRepository.existsById(cmd.getMerchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        StoreAvailability storeAvailability = storeDataMapper.fromCmdToStoreAvailability(cmd, cmd.getStoreId());
        storeAvailability.newStoreAvailability();

        StoreAvailability savedAvailability = storeAvailabilityRepository.save(storeAvailability);

        log.info("AddStoreAvailabilityService - add: END");

        return storeDataMapper.toStoreAvailabilityDto(savedAvailability);
    }
}
