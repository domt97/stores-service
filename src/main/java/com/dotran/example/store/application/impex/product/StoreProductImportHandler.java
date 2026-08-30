package com.dotran.example.store.application.impex.product;

import com.dotran.example.store.application.event.OutboxEventHelper;
import com.dotran.example.store.application.impex.template.AbstractImportTemplate;
import com.dotran.example.store.application.impex.template.ImportResult;
import com.dotran.example.store.application.repository.OutboxEventRepository;
import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.application.repository.StoreRepository;
import com.dotran.example.store.application.repository.TenantRepository;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.ProductSkuId;
import com.dotran.example.store.common.exception.NotFoundException;
import com.dotran.example.store.common.exception.ValidationException;
import com.dotran.example.store.common.utils.CollectionUtils;
import com.dotran.example.store.domain.event.OutboxEvent;
import com.dotran.example.store.domain.model.ProductImage;
import com.dotran.example.store.domain.model.ProductSku;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.domain.model.TenantInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreProductImportHandler extends
        AbstractImportTemplate<ProductImportContext, ProductImportData, ProductImportRequest> {

    private final TenantRepository tenantRepository;
    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventHelper outboxEventHelper;

    @Override
    protected void validate(ProductImportContext context, ProductImportData data) {
        log.info("Validation Product Import - START");

        TenantInfo tenantInfo = tenantRepository.findByTenantId(context.getTenantId())
                .orElseThrow(() -> new NotFoundException("Not found Tenant"));

        if (!storeRepository.existsByTenantIdAndStoreId(tenantInfo.getId(), context.getStoreId())) {
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

        log.info("Validation Product Import - END, {} products validated.", productRefRowMap.size());
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
                .createOutboxEvents(context.getTenantId(), context.getProducts());
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
