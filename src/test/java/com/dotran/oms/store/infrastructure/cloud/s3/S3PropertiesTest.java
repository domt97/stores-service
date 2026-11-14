package com.dotran.oms.store.infrastructure.cloud.s3;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.impex.product.ImportFileName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("S3Properties")
class S3PropertiesTest {

    @Test
    @DisplayName("import key = {merchantId}/{storeId}/imports/{fileName} (BR-CAT-16)")
    void importKey_layout() {
        MerchantId merchant = MerchantId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        StoreId store = StoreId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        S3Properties properties = new S3Properties("localdev-store-products");

        assertThat(properties.getStoreProductBucket()).isEqualTo("localdev-store-products");
        assertThat(properties.importKey(merchant, store, ImportFileName.of("sản phẩm.xlsx")))
                .isEqualTo("11111111-1111-1111-1111-111111111111/22222222-2222-2222-2222-222222222222/imports/sản phẩm.xlsx");
    }

    @Test
    @DisplayName("two stores of one merchant get different folders")
    void importKey_perStore() {
        MerchantId merchant = MerchantId.newMerchantId();
        S3Properties properties = new S3Properties("b");
        ImportFileName name = ImportFileName.of("f.xlsx");

        assertThat(properties.importKey(merchant, StoreId.newStoreId(), name))
                .isNotEqualTo(properties.importKey(merchant, StoreId.newStoreId(), name));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("a blank bucket setting fails at startup")
    void blankBucket_fails(String bucket) {
        assertThatThrownBy(() -> new S3Properties(bucket))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aws.s3.store-product-bucket");
    }
}
