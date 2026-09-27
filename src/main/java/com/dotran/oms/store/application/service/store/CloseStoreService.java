package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.store.application.command.store.CloseStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.store.CloseStoreUseCase;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CloseStoreService implements CloseStoreUseCase {

    private final StoreRepository storeRepository;
    private final StoreDataMapper storeDataMapper;

    @Override
    @Transactional
    public StoreDetailDto close(CloseStoreCmd cmd) {
        Store store = storeRepository.findByTenantIdAndStoreId(cmd.getTenantId(), cmd.getStoreId())
                .orElseThrow(StoreNotFoundException::new);

        store.close();

        Store closedStore = storeRepository.close(store);

        return storeDataMapper.toStoreDetailDto(closedStore);
    }
}
