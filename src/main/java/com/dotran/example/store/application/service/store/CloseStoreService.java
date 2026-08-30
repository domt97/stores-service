package com.dotran.example.store.application.service.store;

import com.dotran.example.store.application.command.store.CloseStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.mapper.StoreDataMapper;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.usecase.store.CloseStoreUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.exception.StoreNotFoundException;
import com.dotran.example.store.domain.model.Store;
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
