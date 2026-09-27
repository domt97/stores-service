package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.usecase.storeproduct.CreateStoreProductUseCase;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateStoreProductUseCaseService implements CreateStoreProductUseCase {

    private final StoreProductRepository storeProductRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StoreProductMapper storeProductMapper;
    private final OutboxEventHelper outboxEventHelper;

    @Override
    @Transactional
    public StoreProductDetailDto createProduct(CreateStoreProductCmd createStoreProductCmd) {
        StoreProduct storeProduct = storeProductMapper.fromCreateStoreProductCmd(createStoreProductCmd);
        storeProduct.initState();

        StoreProduct createdStoreProduct = storeProductRepository.create(storeProduct);

        OutboxEvent outboxEvent = outboxEventHelper.
                createOutboxEvent(createStoreProductCmd.getTenantId(), createdStoreProduct);
        outboxEventRepository.save(outboxEvent);

        return storeProductMapper.fromStoreProduct(createdStoreProduct);
    }
}
