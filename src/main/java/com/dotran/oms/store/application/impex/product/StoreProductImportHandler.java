package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.io.template.imp.AbstractImportTemplate;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.core.util.CollectionUtils;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.model.ProductImage;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreProductImportHandler extends
        AbstractImportTemplate<ProductImportContext, ProductImportData, ProductImportRequest> {

    private static final int MAX_SKU_CODE_LENGTH = 100;
    private static final int SKU_CODE_CHECK_CHUNK = 500;

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventHelper outboxEventHelper;

    @Override
    protected void validate(ProductImportContext context, ProductImportData data) {
        log.info("Validation Product Import - START");

        if (!merchantRepository.existsById(context.getMerchantId())) {
            throw new NotFoundException("Not found Merchant");
        }

        if (!storeRepository.existsByMerchantIdAndStoreId(context.getMerchantId(), context.getStoreId())) {
            throw new NotFoundException("Not found Store");
        }

        if (CollectionUtils.isEmpty(data.getProducts())) {
            throw new ValidationException("Product list is empty");
        }

        Map<String, ProductImportRow> productRefRowMap = data.getProducts().stream()
                .collect(Collectors.toMap(ProductImportRow::getReference, row -> row));

        Map<String, List<ProductSKUImportRow>> productSKUImportRowMap = data.getSkus().stream()
                .collect(Collectors.groupingBy(ProductSKUImportRow::getProductRef));

        Map<String, List<ProductImageImportRow>> productImageImportRowMap = data.getImages().stream()
                .collect(Collectors.groupingBy(ProductImageImportRow::getProductRef));

        productRefRowMap.forEach((productRef, productRow) -> {
            if (!productSKUImportRowMap.containsKey(productRef)) {
                throw new ValidationException("Missing SKU for product reference: " + productRef);
            }
            if (!productImageImportRowMap.containsKey(productRef)) {
                throw new ValidationException("Missing Image for product reference: " + productRef);
            }
        });

        validateSkuCodes(context, data.getSkus());

        log.info("Validation Product Import - END, {} products validated.", productRefRowMap.size());
    }

    /**
     * SKU codes must be present, at most 100 characters, unique in the file and not used in the store yet (removed SKUs
     * included, BR-CAT-02 / BR-CAT-09). Blank codes would also break the inventory sync of the product.
     */
    private void validateSkuCodes(ProductImportContext context, List<ProductSKUImportRow> skuRows) {
        Set<String> codes = new HashSet<>();
        for (ProductSKUImportRow row : skuRows) {
            String code = row.getSku() != null ? row.getSku().getValue() : null;
            if (code == null || code.isBlank() || code.length() > MAX_SKU_CODE_LENGTH) {
                throw new ValidationException("Invalid SKU code for product reference: " + row.getProductRef());
            }
            if (!codes.add(code)) {
                throw new ValidationException("SKU code " + code + " appears more than once in the file");
            }
        }

        List<SKU> all = codes.stream().map(SKU::of).toList();
        for (int from = 0; from < all.size(); from += SKU_CODE_CHECK_CHUNK) {
            Set<SKU> inUse = storeProductRepository.findSkuCodesInStore(context.getStoreId(),
                    all.subList(from, Math.min(from + SKU_CODE_CHECK_CHUNK, all.size())));
            if (!inUse.isEmpty()) {
                throw new SkuCodeInUseException(String.format(Constants.ERROR_MSG_SKU_CODE_IN_USE,
                        inUse.stream().map(SKU::getValue).sorted().collect(Collectors.joining(", "))));
            }
        }
    }

    @Override
    protected ProductImportRequest transform(ProductImportContext context, ProductImportData data) {
        log.info("Transforming Product Import Data to Request - START");
        Map<String, ProductImportRow> productRefRowMap = data.getProducts().stream()
                .collect(Collectors.toMap(ProductImportRow::getReference, row -> row));

        Map<String, List<ProductSKUImportRow>> productSKUImportRowMap = data.getSkus().stream()
                .collect(Collectors.groupingBy(ProductSKUImportRow::getProductRef));

        Map<String, List<ProductImageImportRow>> productImageImportRowMap = data.getImages().stream()
                .collect(Collectors.groupingBy(ProductImageImportRow::getProductRef));

        List<StoreProduct> storeProductList = new ArrayList<>();

        for (String productRef : productRefRowMap.keySet()) {
            ProductImportRow productRow = productRefRowMap.get(productRef);
            List<ProductSKUImportRow> skuRowList = productSKUImportRowMap.get(productRef);
            List<ProductImageImportRow> imageRowList = productImageImportRowMap.get(productRef);

            ProductId productId = ProductId.newProductId();
            StoreProduct storeProduct = StoreProduct.builder()
                    .id(productId)
                    .storeId(context.getStoreId())
                    .name(productRow.getName())
                    .description(productRow.getDescription())
                    .thumbnailUrl(productRow.getThumbnailUrl())
                    .categoryId(productRow.getCategoryId())
                    .skus(transformSkuRows(productId, skuRowList))
                    .images(transformImageRows(productId, imageRowList))
                    .build();

            storeProduct.initState();

            storeProductList.add(storeProduct);
        }

        log.info("Transforming Product Import Data to Request - END, {} products transformed.", storeProductList.size());

        return ProductImportRequest.builder()
                .products(storeProductList)
                .build();
    }

    @Override
    @Transactional
    protected ImportResult process(ProductImportContext context, ProductImportRequest request) {
        log.info("Processing Product Import - START");
        List<StoreProduct> storeProductList = request.getProducts();

        List<StoreProduct> savedProductList = storeProductRepository.saveAll(storeProductList);
        context.setProducts(savedProductList);

        log.info("Processing Product Import - END, {} products imported.", savedProductList.size());

        return ImportResult.builder()
                .totalRecords(storeProductList.size())
                .status(ImportResult.ImportStatus.SUCCESS)
                .build();
    }

    @Override
    protected void postImport(ProductImportContext context, ProductImportRequest request, ImportResult importResult) {
        log.info("Post Import Product - START");
        if (CollectionUtils.isEmpty(context.getProducts())) {
            log.warn("No products were imported, skipping post-import processing.");
            return;
        }

        List<OutboxEvent> outboxEvents = outboxEventHelper
                .createProductCreatedOutboxEvents(context.getMerchantId(), context.getProducts());
        outboxEventRepository.saveAll(outboxEvents);
        log.info("Post Import Product - END, {} outbox events created.", outboxEvents.size());
    }

    private List<ProductSku> transformSkuRows(ProductId productId, List<ProductSKUImportRow> skuRows) {
        return skuRows.stream()
                .map(skuRow -> {
                    ProductSku productSku = ProductSku.builder()
                            .id(ProductSkuId.newProductSkuId())
                            .productId(productId)
                            .sku(skuRow.getSku())
                            .name(skuRow.getName())
                            .price(skuRow.getPrice())
                            .currency(skuRow.getCurrency())
                            .weight(skuRow.getWeight())
                            .length(skuRow.getLength())
                            .width(skuRow.getWidth())
                            .height(skuRow.getHeight())
                            .build();
                    productSku.init();

                    return productSku;
                })
                .collect(Collectors.toList());
    }

    private List<ProductImage> transformImageRows(ProductId productId, List<ProductImageImportRow> imageRows) {
        AtomicInteger imageIndex = new AtomicInteger(1);
        return imageRows.stream()
                .map(imageRow -> {
                    ProductImage productImage = ProductImage.builder()
                            .productId(productId)
                            .imageUrl(imageRow.getImageUrl())
                            .displayOrder(imageIndex.getAndIncrement())
                            .build();
                    productImage.init();

                    return productImage;
                })
                .collect(Collectors.toList());
    }
}
