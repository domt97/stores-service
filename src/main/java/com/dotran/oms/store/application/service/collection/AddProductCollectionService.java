package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.usecase.collection.AddProductCollectionUseCase;
import com.dotran.oms.store.domain.model.StoreCollection;
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
