package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.AddProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.collection.AddProductCollectionUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.StoreCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class AddProductCollectionService implements AddProductCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreProductRepository storeProductRepository;
    private final StoreRepository storeRepository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto addProductsToCollection(AddProductCollectionCmd cmd) {
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        // Existence only: the write is a single statement and the answer is re-read after it.
        if (!repository.existsByStoreIdAndId(cmd.getStoreId(), cmd.getStoreCollectionId())) {
            throw new NotFoundException(Constants.ERROR_MSG_COLLECTION_NOT_FOUND);
        }

        List<ProductId> productIds = CollectionProductIds.distinct(cmd.getProductIds());
        StoreCollection.checkProductListChange(productIds);

        // A collection contains only products of its own store; another store's product is "not found".
        if (storeProductRepository.findExistingProductIds(cmd.getStoreId(), productIds).size() < productIds.size()) {
            throw new NotFoundException(Constants.ERROR_MSG_PRODUCT_NOT_FOUND);
        }

        // Idempotent (BR-CAT-15): every requested id goes to the insert, which skips products already in the collection
        // (also those added by a concurrent request). Never filter against the loaded snapshot: it may be stale.
        StoreCollection updatedCollection = repository.addProducts(cmd.getStoreId(), cmd.getStoreCollectionId(), productIds, Instant.now());

        return mapper.toCollectionDto(updatedCollection);
    }
}
