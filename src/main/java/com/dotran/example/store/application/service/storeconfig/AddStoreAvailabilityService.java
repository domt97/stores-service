package com.dotran.example.store.application.service.storeconfig;

import com.dotran.example.store.application.command.storeconfig.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.dto.StoreAvailabilityDto;
import com.dotran.example.store.application.mapper.StoreDataMapper;
import com.dotran.example.store.application.repository.StoreAvailabilityRepository;
import com.dotran.example.store.application.usecase.AddStoreAvailabilityUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.domain.model.StoreAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AddStoreAvailabilityService implements AddStoreAvailabilityUseCase {

    private final StoreAvailabilityRepository storeAvailabilityRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreAvailabilityDto add(AddStoreAvailabilityCmd cmd) {
        log.info("AddStoreAvailabilityService - add: START for storeId={}", cmd.getStoreId());
        
        StoreAvailability storeAvailability = storeDataMapper.fromCmdToStoreAvailability(cmd, StoreId.of(cmd.getStoreId()));
        storeAvailability.newStoreAvailability();

        StoreAvailability savedAvailability = storeAvailabilityRepository.save(storeAvailability);

        log.info("AddStoreAvailabilityService - add: END");

        return storeDataMapper.toStoreAvailabilityDto(savedAvailability);
    }
}
