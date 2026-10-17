package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.usecase.collection.CreateStoreCollectionUseCase;
import com.dotran.oms.store.domain.model.StoreCollection;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class CreateStoreCollectionService implements CreateStoreCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreProductRepository storeProductRepository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto create(CreateStoreCollectionCmd cmd) {
        StoreCollection storeCollection = mapper.fromCreateCmd(cmd);
        storeCollection.init();

        List<StoreProduct> storeProducts = storeProductRepository.getProductsByListOfProductIds(cmd.getProductIds());

        for (StoreProduct product : storeProducts) {
            if (!product.getStoreId().equals(cmd.getStoreId())) {
                throw new BusinessException("Cannot add product from a different store to the collection");
            }
        }

        cmd.getProductIds().forEach(storeCollection::addProduct);

        StoreCollection savedCollection = repository.create(storeCollection);

        return mapper.toCollectionDto(savedCollection);
    }
}
