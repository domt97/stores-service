package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.storeproduct.UpdateProductSkuCmd;
import com.dotran.oms.store.application.command.storeproduct.UpdateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.storeproduct.UpdateStoreProductUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.exception.ProductRuleViolationException;
import com.dotran.oms.store.domain.exception.ProductVersionConflictException;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.model.ProductChangeSet;
import com.dotran.oms.store.domain.model.ProductReplacement;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.ProductSkuSpec;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Update-product (full replace, oms-workspace flows/product-updated.md).
 * <p>
 * Order matters: the change set is computed on the domain object first (nothing in the persistence context is dirty),
 * then the version is checked and bumped with a guarded UPDATE that locks the product row, then SKU codes are checked
 * against other products (a JPQL query, so before any entity is changed), then the change set is applied and flushed,
 * then ProductUpdated is written to the outbox in the same transaction with the bumped version.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateStoreProductService implements UpdateStoreProductUseCase {

    private final StoreProductRepository repository;
    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StoreProductMapper mapper;
    private final OutboxEventHelper outboxEventHelper;

    @Override
    @Transactional
    public StoreProductDetailDto updateProduct(UpdateStoreProductCmd cmd) {
        if (!merchantRepository.existsById(cmd.getMerchantId())) {
            throw new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        }
        Store store = storeRepository.findByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND));
        StoreProduct product = repository.getByStoreIdAndProductId(store.getId(), cmd.getProductId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_PRODUCT_NOT_FOUND));

        long readVersion = cmd.getVersion();
        ProductChangeSet changes;
        try {
            changes = product.replace(toReplacement(cmd), Instant.now());
        } catch (ValidationException | ProductRuleViolationException e) {
            // Judged against a state the client has not seen: report the conflict, not the rule.
            if (product.getVersion() != readVersion) {
                throw versionConflict(product.getVersion());
            }
            throw e;
        }

        if (changes.isEmpty()) {
            // Already in the requested state (e.g. a retry of a committed update): nothing written, no event.
            // The product row and its SKUs/images are read by separate statements; if another update committed in
            // between, the "same state" was judged on a mix of two versions.
            if (repository.currentVersion(store.getId(), product.getId()) != product.getVersion()) {
                throw new ProductVersionConflictException(Constants.ERROR_MSG_PRODUCT_CONCURRENT_UPDATE);
            }
            return mapper.fromStoreProduct(product);
        }
        if (product.getVersion() != readVersion) {
            throw versionConflict(product.getVersion());
        }
        if (!repository.bumpVersion(store.getId(), product.getId(), readVersion, product.getUpdatedAt())) {
            throw new ProductVersionConflictException(Constants.ERROR_MSG_PRODUCT_CONCURRENT_UPDATE);
        }
        long newVersion = readVersion + 1;

        List<SKU> addedCodes = changes.added().stream().map(ProductSku::getSku).toList();
        Set<SKU> inUse = repository.findSkuCodesOfOtherProducts(store.getId(), product.getId(), addedCodes);
        if (!inUse.isEmpty()) {
            throw new SkuCodeInUseException(String.format(Constants.ERROR_MSG_SKU_CODE_IN_USE,
                    inUse.stream().map(SKU::getValue).sorted().collect(Collectors.joining(", "))));
        }

        StoreProduct saved = repository.applyChanges(product, changes, newVersion);
        outboxEventRepository.save(
                outboxEventHelper.createProductUpdatedOutboxEvent(store.getMerchantId(), saved, newVersion));

        log.info("Updated product {} (store {}) to version {}: fields {}, images {}, SKUs +{} ~{} -{}",
                saved.getId().getValue(), store.getId().getValue(), newVersion, changes.productFieldsChanged(),
                changes.imagesChanged(), changes.added().size(), changes.updated().size(), changes.removed().size());

        return mapper.fromStoreProduct(saved);
    }

    private static ProductVersionConflictException versionConflict(long currentVersion) {
        return new ProductVersionConflictException(
                String.format(Constants.ERROR_MSG_PRODUCT_VERSION_CONFLICT, currentVersion));
    }

    private static ProductReplacement toReplacement(UpdateStoreProductCmd cmd) {
        List<ProductSkuSpec> skus = cmd.getSkus() == null ? List.of() : cmd.getSkus().stream()
                .map(UpdateStoreProductService::toSpec)
                .toList();
        List<ProductReplacement.ImageSpec> images = cmd.getImages() == null ? List.of() : cmd.getImages().stream()
                .map(image -> new ProductReplacement.ImageSpec(image.getImageUrl(), image.getDisplayOrder()))
                .toList();
        return new ProductReplacement(cmd.getName(), cmd.getDescription(), cmd.getThumbnailUrl(), cmd.getCategoryId(),
                cmd.getStatus(), skus, images);
    }

    private static ProductSkuSpec toSpec(UpdateProductSkuCmd sku) {
        return new ProductSkuSpec(sku.getId() != null ? ProductSkuId.of(sku.getId()) : null, sku.getSku(), sku.getName(),
                sku.getPrice(), sku.getCurrency(), sku.getWeight(), sku.getLength(), sku.getWidth(), sku.getHeight(),
                sku.getStatus());
    }
}
