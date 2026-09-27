package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.store.application.command.store.GetStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.store.GetStoreUseCase;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetStoreService implements GetStoreUseCase {

    private final StoreRepository storeRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreDetailDto getStoreByTenantIdAndStoreId(GetStoreCmd cmd) {
        Store store = storeRepository.findByTenantIdAndStoreId(cmd.getTenantId(), cmd.getStoreId())
                .orElseThrow(StoreNotFoundException::new);

        return storeDataMapper.toStoreDetailDto(store);
    }
}
