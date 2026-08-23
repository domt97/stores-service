package com.dotran.example.store.application.service.collection;

import com.dotran.example.store.application.command.collection.GetListCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.mapper.StoreCollectionMapper;
import com.dotran.example.store.application.repository.StoreCollectionRepository;
import com.dotran.example.store.application.usecase.collection.GetListStoreCollectionUseCase;
import com.dotran.example.store.common.annotation.UseCase;
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
