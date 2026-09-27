package com.dotran.oms.store.application.service.collection;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.ProductId;
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
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<ProductId, StoreProduct> productMap = storeProducts.stream()
                .collect(Collectors.toMap(StoreProduct::getId, product -> product));

        for(ProductId productId : cmd.getProductIds()) {
            storeCollection.addProduct(productMap.get(productId));
        }

        StoreCollection savedCollection = repository.create(storeCollection);

        return mapper.toCollectionDto(savedCollection);
    }
}
