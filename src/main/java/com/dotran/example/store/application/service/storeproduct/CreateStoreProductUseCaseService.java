package com.dotran.example.store.application.service.storeproduct;

import com.dotran.example.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.example.store.application.dto.StoreProductDetailDto;
import com.dotran.example.store.application.mapper.StoreProductMapper;
import com.dotran.example.store.application.repository.OutboxEventRepository;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.usecase.storeproduct.CreateStoreProductUseCase;
import com.dotran.example.store.common.annotation.UseCase;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.event.OutboxEvent;
import com.dotran.example.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateStoreProductUseCaseService implements CreateStoreProductUseCase {

    private final StoreProductRepository storeProductRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StoreProductMapper storeProductMapper;

    @Override
    @Transactional
    public StoreProductDetailDto createProduct(CreateStoreProductCmd createStoreProductCmd) {
        StoreProduct storeProduct = storeProductMapper.fromCreateStoreProductCmd(createStoreProductCmd);
        storeProduct.initState();

        StoreProduct createdStoreProduct = storeProductRepository.create(storeProduct);

        OutboxEvent outboxEvent = createdStoreProduct.toOutboxEvent(TenantId.of(createStoreProductCmd.getTenantId()));
        outboxEventRepository.save(outboxEvent);

        return storeProductMapper.fromStoreProduct(createdStoreProduct);
    }
}
