package com.dotran.example.store.application.service.store;

import com.dotran.example.store.application.command.store.GetStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.mapper.StoreDataMapper;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.usecase.GetStoreUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.exception.StoreNotFoundException;
import com.dotran.example.store.domain.model.Store;
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
        Store store = storeRepository.findByTenantIdAndStoreId(TenantId.of(cmd.getTenantId()), StoreId.of(cmd.getStoreId()))
                .orElseThrow(StoreNotFoundException::new);

        return storeDataMapper.toStoreDetailDto(store);
    }
}
