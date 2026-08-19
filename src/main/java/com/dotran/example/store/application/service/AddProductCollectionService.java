package com.dotran.example.store.application.service;

import com.dotran.example.store.application.command.AddProductCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.mapper.StoreCollectionMapper;
import com.dotran.example.store.application.repository.StoreCollectionRepository;
import com.dotran.example.store.application.usecase.AddProductCollectionUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.domain.model.StoreCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AddProductCollectionService implements AddProductCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto addProductsToCollection(AddProductCollectionCmd cmd) {
        StoreCollection storeCollection = repository.getById(cmd.getStoreCollectionId())
                .orElseThrow(() -> new NotFoundException("Collection not found"));

        if (storeCollection.canAddProductsToCollection(cmd.getProductIds())) {
            storeCollection.update();
        }

        StoreCollection updatedCollection = repository.addProducts(storeCollection, cmd.getProductIds());

        return mapper.toCollectionDto(updatedCollection);
    }
}
