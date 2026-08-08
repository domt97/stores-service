package com.dotran.example.store.application.service;

import com.dotran.example.store.application.repository.StoreAvailabilityRepository;
import com.dotran.example.store.application.usecase.CancelStoreAvailabilityUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreAvailabilityId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.StoreAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelStoreAvailabilityService implements CancelStoreAvailabilityUseCase {

    private final StoreAvailabilityRepository storeAvailabilityRepository;

    @Override
    public void cancel(UUID storeAvailabilityId, UUID storeId) {
        log.info("CancelStoreAvailabilityService - cancel: START for storeId={}", storeId);

        StoreAvailability storeAvailability = storeAvailabilityRepository.findByIdAndStoreId(
                StoreAvailabilityId.of(storeAvailabilityId),
                StoreId.of(storeId)
        ).orElseThrow(() -> new NotFoundException("Store availability not found"));

        storeAvailability.cancel();

        storeAvailabilityRepository.save(storeAvailability);

        log.info("CancelStoreAvailabilityService - cancel: END");
    }
}
