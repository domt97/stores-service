package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.store.application.command.collection.GetListCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.usecase.collection.GetListStoreCollectionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetListStoreCollectionService implements GetListStoreCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public List<StoreCollectionDto> getListCollectionByStoreId(GetListCollectionCmd cmd) {
        return repository
                .getListCollectionByStoreId(cmd.getStoreId())
                .stream()
                .map(mapper::toCollectionDto)
                .toList();
    }
}
