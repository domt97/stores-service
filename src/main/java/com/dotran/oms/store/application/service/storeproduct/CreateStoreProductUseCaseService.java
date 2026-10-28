package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.storeproduct.CreateStoreProductUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateStoreProductUseCaseService implements CreateStoreProductUseCase {

    private final StoreProductRepository storeProductRepository;
    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StoreProductMapper storeProductMapper;
    private final OutboxEventHelper outboxEventHelper;

    @Override
    @Transactional
    public StoreProductDetailDto createProduct(CreateStoreProductCmd createStoreProductCmd) {
        if (!merchantRepository.existsById(createStoreProductCmd.getMerchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }

        // The store must belong to the merchant; after this check the path ids are the verified owner and store.
        if (!storeRepository.existsByMerchantIdAndStoreId(createStoreProductCmd.getMerchantId(), createStoreProductCmd.getStoreId())) {
            throw new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND);
        }

        StoreProduct storeProduct = storeProductMapper.fromCreateStoreProductCmd(createStoreProductCmd);
        storeProduct.initState();

        StoreProduct createdStoreProduct = storeProductRepository.create(storeProduct);

        OutboxEvent outboxEvent = outboxEventHelper.
                createProductCreatedOutboxEvent(createStoreProductCmd.getMerchantId(), createdStoreProduct);
        outboxEventRepository.save(outboxEvent);

        return storeProductMapper.fromStoreProduct(createdStoreProduct);
    }
}
