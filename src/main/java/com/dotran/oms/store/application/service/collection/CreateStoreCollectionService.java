package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.collection.CreateStoreCollectionUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.StoreCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class CreateStoreCollectionService implements CreateStoreCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreProductRepository storeProductRepository;
    private final StoreRepository storeRepository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto create(CreateStoreCollectionCmd cmd) {
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        List<ProductId> productIds = CollectionProductIds.distinct(cmd.getProductIds());

        // A collection contains only products of its own store; another store's product is "not found".
        if (!productIds.isEmpty()
                && storeProductRepository.findExistingProductIds(cmd.getStoreId(), productIds).size() < productIds.size()) {
            throw new NotFoundException(Constants.ERROR_MSG_PRODUCT_NOT_FOUND);
        }

        StoreCollection storeCollection = mapper.fromCreateCmd(cmd);
        storeCollection.init();
        productIds.forEach(storeCollection::addProduct);

        StoreCollection savedCollection = repository.create(storeCollection);

        return mapper.toCollectionDto(savedCollection);
    }
}
