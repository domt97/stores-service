package com.dotran.oms.store.application.impex.product;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.io.template.imp.AbstractImportTemplate;
import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.core.util.CollectionUtils;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Validates and saves the rows of an Excel import. Precondition: the caller (ImportProductService) has checked that the
 * merchant exists and the store belongs to it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreProductImportHandler extends
        AbstractImportTemplate<ProductImportContext, ProductImportData, ProductImportRequest> {

    private static final int MAX_SKU_CODE_LENGTH = 100;
    private static final int SKU_CODE_CHECK_CHUNK = 500;
    private static final int MAX_TEXT_COLUMN_LENGTH = 255;
    private static final BigDecimal MAX_DIMENSION_EXCLUSIVE = new BigDecimal("100000000");

    private final StoreProductRepository storeProductRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventHelper outboxEventHelper;
    private final TransactionTemplate transactionTemplate;

    @Override
    protected void validate(ProductImportContext context, ProductImportData data) {
        log.info("Validation Product Import - START");

        if (CollectionUtils.isEmpty(data.getProducts())) {
            throw new ValidationException("Product list is empty");
        }

        validateReferences(data);
        validateColumnSizes(data);

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

        warnAboutOrphanRows(context, productRefRowMap.keySet(), productSKUImportRowMap.keySet(), "SKU");
        warnAboutOrphanRows(context, productRefRowMap.keySet(), productImageImportRowMap.keySet(), "image");

        validateSkuCodes(context, data.getSkus());

        log.info("Validation Product Import - END, {} products validated.", productRefRowMap.size());
    }

    /**
     * Every row needs a product reference (compared exactly as written, not trimmed), and a product reference appears
     * once in the Product sheet. Checked before the rows are grouped by reference.
     */
    private void validateReferences(ProductImportData data) {
        Set<String> productRefs = new HashSet<>();
        for (ProductImportRow row : data.getProducts()) {
            if (isBlank(row.getReference())) {
                throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_REFERENCE_REQUIRED, "Product"));
            }
            if (!productRefs.add(row.getReference())) {
                throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_REFERENCE_DUPLICATE, row.getReference()));
            }
        }
        if (data.getSkus().stream().anyMatch(row -> isBlank(row.getProductRef()))) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_REFERENCE_REQUIRED, "Product SKU"));
        }
        if (data.getImages().stream().anyMatch(row -> isBlank(row.getProductRef()))) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_REFERENCE_REQUIRED, "Product Image"));
        }
    }

    /**
     * Values the database would reject, checked here so the seller gets a clear message instead of a database error:
     * thumbnail URL and SKU name ≤ 255 characters, image URL required, weight and dimensions within NUMERIC(10,2)
     * (≥ 0, ≤ 2 decimals, below 10^8). Product name and price are checked by the domain (initState).
     */
    private void validateColumnSizes(ProductImportData data) {
        for (ProductImportRow row : data.getProducts()) {
            checkLength("Thumbnail URL", row.getThumbnailUrl(), row.getReference());
        }
        for (ProductSKUImportRow row : data.getSkus()) {
            checkLength("SKU name", row.getName(), row.getProductRef());
            String code = row.getSku() == null ? null : row.getSku().getValue();
            checkDimension("Weight", row.getWeight(), code);
            checkDimension("Length", row.getLength(), code);
            checkDimension("Width", row.getWidth(), code);
            checkDimension("Height", row.getHeight(), code);
        }
        for (ProductImageImportRow row : data.getImages()) {
            if (isBlank(row.getImageUrl())) {
                throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_IMAGE_URL_REQUIRED, row.getProductRef()));
            }
        }
    }

    private static void checkLength(String field, String value, String productRef) {
        if (value != null && value.length() > MAX_TEXT_COLUMN_LENGTH) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_FIELD_TOO_LONG, field,
                    MAX_TEXT_COLUMN_LENGTH, productRef));
        }
    }

    private static void checkDimension(String field, BigDecimal value, String skuCode) {
        if (value != null && (value.signum() < 0 || value.stripTrailingZeros().scale() > 2
                || value.compareTo(MAX_DIMENSION_EXCLUSIVE) >= 0)) {
            throw new ValidationException(String.format(Constants.ERROR_MSG_IMPORT_DIMENSION_INVALID, field, skuCode));
        }
    }

    /**
     * Rows whose product reference has no product row are not imported (decided 2026-10-30: keep, but say so). Their
     * SKU codes are still validated with the rest of the file.
     */
    private static void warnAboutOrphanRows(ProductImportContext context, Set<String> productRefs, Set<String> rowRefs, String kind) {
        List<String> orphans = rowRefs.stream().filter(ref -> !productRefs.contains(ref)).sorted().toList();
        if (!orphans.isEmpty()) {
            log.warn("Import merchant={} store={}: {} rows of {} product references have no product row and are not imported "
                            + "(their SKU codes are still validated): {}",
                    context.getMerchantId(), context.getStoreId(), kind, orphans.size(), orphans.subList(0, Math.min(10, orphans.size())));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    /**
     * Saves the products and their ProductCreated outbox rows in ONE transaction: any failure rolls both back, so
     * products are never stored without their events (workspace rule 2). The template calls this method on
     * {@code this}, which bypasses the Spring proxy, so {@code @Transactional} would be ignored here — the transaction
     * is opened with {@link TransactionTemplate}. Validation and transform run before it, outside.
     */
    @Override
    protected ImportResult process(ProductImportContext context, ProductImportRequest request) {
        log.info("Processing Product Import - START");
        List<StoreProduct> storeProductList = request.getProducts();

        List<StoreProduct> savedProductList = transactionTemplate.execute(status -> {
            List<StoreProduct> saved = storeProductRepository.saveAll(storeProductList);
            if (CollectionUtils.isEmpty(saved)) {
                log.warn("No products were imported, no outbox events written.");
                return saved;
            }
            List<OutboxEvent> outboxEvents = outboxEventHelper.createProductCreatedOutboxEvents(context.getMerchantId(), saved);
            outboxEventRepository.saveAll(outboxEvents);
            log.info("{} ProductCreated outbox events written.", outboxEvents.size());
            return saved;
        });
        context.setProducts(savedProductList);

        log.info("Processing Product Import - END, {} products imported.", savedProductList.size());

        return ImportResult.builder()
                .totalRecords(storeProductList.size())
                .status(ImportResult.ImportStatus.SUCCESS)
                .build();
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
