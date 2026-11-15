package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.store.CancelStoreAvailabilityCmd;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.store.CancelStoreAvailabilityUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.StoreAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels an availability window. The store must belong to the path merchant (system rule 4: windows are scoped through
 * their store, which never moves to another merchant), then the window must belong to that store; otherwise 404 and
 * nothing is read or written.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelStoreAvailabilityService implements CancelStoreAvailabilityUseCase {

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreAvailabilityRepository storeAvailabilityRepository;

    @Override
    @Transactional
    public void cancel(CancelStoreAvailabilityCmd cmd) {
        log.info("CancelStoreAvailabilityService - cancel: START");
        if (!merchantRepository.existsById(cmd.merchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.merchantId(), cmd.storeId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        StoreAvailability storeAvailability = storeAvailabilityRepository
                .findByIdAndStoreId(cmd.storeAvailabilityId(), cmd.storeId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_AVAILABILITY_NOT_FOUND));

        storeAvailability.cancel();

        storeAvailabilityRepository.save(storeAvailability);

        log.info("CancelStoreAvailabilityService - cancel: END");
    }
}
