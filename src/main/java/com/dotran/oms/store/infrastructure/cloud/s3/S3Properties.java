package com.dotran.oms.store.infrastructure.cloud.s3;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Where stores-service keeps its files in S3. The bucket comes only from configuration
 * ({@code aws.s3.store-product-bucket}, env {@code AWS_S3_STORE_PRODUCT_BUCKET}); a client never chooses it. Import files
 * live at {@code {merchantId}/{storeId}/imports/{fileName}} (BR-CAT-16; merchant-scoped layout, sa-aws-platform rule 8).
 */
@Slf4j
@Getter
@Component
public class S3Properties {

    private final String storeProductBucket;

    public S3Properties(@Value("${aws.s3.store-product-bucket}") String storeProductBucket) {
        if (storeProductBucket == null || storeProductBucket.isBlank()) {
            throw new IllegalStateException("aws.s3.store-product-bucket must be set (AWS_S3_STORE_PRODUCT_BUCKET)");
        }
        this.storeProductBucket = storeProductBucket.trim();
        log.info("Store product files: S3 bucket {}", this.storeProductBucket);
    }

    /** Key of an import file in the store's import folder; built only from typed ids and a checked file name. */
    public String importKey(MerchantId merchantId, StoreId storeId, ImportFileName fileName) {
        return merchantId.getValue() + "/" + storeId.getValue() + "/imports/" + fileName.value();
    }
}
