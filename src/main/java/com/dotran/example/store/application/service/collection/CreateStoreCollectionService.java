package com.dotran.example.store.application.service.collection;

import com.dotran.example.store.application.command.collection.CreateStoreCollectionCmd;
import com.dotran.example.store.application.dto.StoreCollectionDto;
import com.dotran.example.store.application.mapper.StoreCollectionMapper;
import com.dotran.example.store.application.repository.StoreCollectionRepository;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.usecase.CreateStoreCollectionUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.domain.model.StoreCollection;
import com.dotran.example.store.domain.model.StoreProduct;
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
