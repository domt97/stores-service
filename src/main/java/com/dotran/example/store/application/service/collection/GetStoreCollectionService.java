package com.dotran.example.store.application.service.collection;

import com.dotran.example.store.application.command.collection.GetCollectionDetailCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.mapper.StoreCollectionMapper;
import com.dotran.example.store.application.repository.StoreCollectionRepository;
import com.dotran.example.store.application.usecase.collection.GetStoreCollectionUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.StoreCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetStoreCollectionService implements GetStoreCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto getCollectionById(GetCollectionDetailCmd cmd) {
        StoreCollection storeCollection = repository.getById(cmd.getStoreCollectionId())
                .orElseThrow(() -> new NotFoundException("Collection not found"));

        return mapper.toCollectionDto(storeCollection);
    }
}
