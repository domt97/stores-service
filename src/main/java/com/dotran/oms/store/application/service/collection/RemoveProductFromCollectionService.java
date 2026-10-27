package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.collection.RemoveProductCollectionCmd;
import com.dotran.oms.store.application.dto.StoreCollectionDto;
import com.dotran.oms.store.application.mapper.StoreCollectionMapper;
import com.dotran.oms.store.application.repository.StoreCollectionRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.collection.RemoveProductFromCollectionUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.model.StoreCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class RemoveProductFromCollectionService implements RemoveProductFromCollectionUseCase {

    private final StoreCollectionRepository repository;
    private final StoreRepository storeRepository;
    private final StoreCollectionMapper mapper;

    @Override
    @Transactional
    public StoreCollectionDto removeProducts(RemoveProductCollectionCmd cmd) {
        if (!storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        StoreCollection storeCollection = repository.getByStoreIdAndId(cmd.getStoreId(), cmd.getStoreCollectionId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_COLLECTION_NOT_FOUND));

        List<ProductId> productIds = CollectionProductIds.distinct(cmd.getProductIds());

        if (storeCollection.canRemoveProductsFromCollection(productIds)) {
            storeCollection.update();
        }

        StoreCollection updatedStoreCollection = repository.removeProducts(storeCollection, productIds);

        return mapper.toCollectionDto(updatedStoreCollection);
    }
}
