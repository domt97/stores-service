package com.dotran.example.store.application.service;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.mapper.StoreProductMapper;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.usecase.CreateStoreProductUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateStoreProductUseCaseService implements CreateStoreProductUseCase {

    private final StoreProductRepository storeProductRepository;
    private final StoreProductMapper storeProductMapper;

    @Override
    public StoreProductDetailDto createProduct(CreateStoreProductCmd createStoreProductCmd) {
        StoreProduct storeProduct = storeProductMapper.fromCreateStoreProductCmd(createStoreProductCmd);

        StoreProduct createdStoreProduct = storeProductRepository.create(storeProduct);

        return storeProductMapper.fromStoreProduct(createdStoreProduct);
    }
}
