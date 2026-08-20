package com.dotran.example.store.application.service;

import com.dotran.example.store.application.command.RemoveProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.mapper.StoreCollectionMapper;
import com.dotran.example.store.application.repository.StoreCollectionRepository;
import com.dotran.example.store.application.usecase.RemoveProductFromCollectionUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.StoreCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RemoveProductFromCollectionService implements RemoveProductFromCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto removeProducts(RemoveProductCollectionCmd cmd) {
        StoreCollection storeCollection = repository.getById(cmd.getStoreCollectionId())
                .orElseThrow(() -> new NotFoundException("Collection not found"));

        if (storeCollection.canRemoveProductsFromCollection(cmd.getProductIds())) {
            storeCollection.update();
        }

        StoreCollection updatedStoreCollection = repository.removeProducts(storeCollection, cmd.getProductIds());

        return mapper.toCollectionDto(updatedStoreCollection);
    }
}
