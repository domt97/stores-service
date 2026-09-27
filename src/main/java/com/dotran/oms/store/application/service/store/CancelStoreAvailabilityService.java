package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.application.usecase.store.CancelStoreAvailabilityUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.model.StoreAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelStoreAvailabilityService implements CancelStoreAvailabilityUseCase {

    private final StoreAvailabilityRepository storeAvailabilityRepository;

    @Override
    @Transactional
    public void cancel(UUID storeAvailabilityId, UUID storeId) {
        log.info("CancelStoreAvailabilityService - cancel: START for storeId={}", storeId);

        StoreAvailability storeAvailability = storeAvailabilityRepository.findByIdAndStoreId(
                StoreAvailabilityId.of(storeAvailabilityId),
                StoreId.of(storeId)
        ).orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_AVAILABILITY_NOT_FOUND));

        storeAvailability.cancel();

        storeAvailabilityRepository.save(storeAvailability);

        log.info("CancelStoreAvailabilityService - cancel: END");
    }
}
