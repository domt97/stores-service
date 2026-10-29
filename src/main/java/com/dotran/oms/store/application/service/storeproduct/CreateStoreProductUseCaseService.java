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
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.dotran.oms.core.domain.id.SKU;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

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

        // BR-CAT-02 / BR-CAT-09: a SKU code is unique within the store, removed SKUs included.
        Set<SKU> inUse = storeProductRepository.findSkuCodesInStore(createStoreProductCmd.getStoreId(),
                storeProduct.getSkus().stream().map(ProductSku::getSku).toList());
        if (!inUse.isEmpty()) {
            throw new SkuCodeInUseException(String.format(Constants.ERROR_MSG_SKU_CODE_IN_USE,
                    inUse.stream().map(SKU::getValue).sorted().collect(Collectors.joining(", "))));
        }

        StoreProduct createdStoreProduct = storeProductRepository.create(storeProduct);

        OutboxEvent outboxEvent = outboxEventHelper.
                createProductCreatedOutboxEvent(createStoreProductCmd.getMerchantId(), createdStoreProduct);
        outboxEventRepository.save(outboxEvent);

        return storeProductMapper.fromStoreProduct(createdStoreProduct);
    }
}
