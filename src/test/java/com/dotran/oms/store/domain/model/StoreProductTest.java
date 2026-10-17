package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoreProduct")
class StoreProductTest {

    private StoreProduct buildProduct() {
        return StoreProduct.builder()
                .id(ProductId.newProductId())
                .storeId(StoreId.newStoreId())
                .name("Test Product")
                .build();
    }

    private ProductSku buildSku(ProductId productId, BigDecimal price, String currency) {
        return ProductSku.builder()
                .id(ProductSkuId.newProductSkuId())
                .productId(productId)
                .sku(SKU.of("SKU-001"))
                .name("Variant")
                .price(price)
                .currency(currency)
                .build();
    }

    @Nested
    @DisplayName("initState")
    class InitState {

        @Test
        @DisplayName("sets status to ACTIVE")
        void setsStatusActive() {
            StoreProduct product = buildProduct();
            product.initState();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("initializes null skus to empty list")
        void initializesNullSkus() {
            StoreProduct product = buildProduct();
            product.initState();
            assertThat(product.getSkus()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("initializes null images to empty list")
        void initializesNullImages() {
            StoreProduct product = buildProduct();
            product.initState();
            assertThat(product.getImages()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("preserves existing skus list when already set")
        void preservesExistingSkus() {
            ProductId productId = ProductId.newProductId();
            ProductSku sku = buildSku(productId, BigDecimal.TEN, "USD");
            StoreProduct product = StoreProduct.builder()
                    .id(productId)
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .skus(new ArrayList<>(List.of(sku)))
                    .build();
            product.initState();
            assertThat(product.getSkus()).hasSize(1);
        }

        @Test
        @DisplayName("sets createdAt and updatedAt")
        void setsTimestamps() {
            StoreProduct product = buildProduct();
            product.initState();
            assertThat(product.getCreatedAt()).isNotNull();
            assertThat(product.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class StatusTransitions {

        @Test
        @DisplayName("activate sets status to ACTIVE")
        void activate_setsStatusActive() {
            StoreProduct product = StoreProduct.builder()
                    .id(ProductId.newProductId())
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .status(ProductStatus.INACTIVE)
                    .build();
            product.activate();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("deactivate sets status to INACTIVE")
        void deactivate_setsStatusInactive() {
            StoreProduct product = StoreProduct.builder()
                    .id(ProductId.newProductId())
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .status(ProductStatus.ACTIVE)
                    .build();
            product.deactivate();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("addSku / addImage")
    class AddItems {

        @Test
        @DisplayName("addSku appends a SKU to the list")
        void addSku_appends() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = StoreProduct.builder()
                    .id(productId)
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .skus(new ArrayList<>())
                    .build();
            ProductSku sku = buildSku(productId, BigDecimal.TEN, "USD");
            product.addSku(sku);
            assertThat(product.getSkus()).containsExactly(sku);
        }

        @Test
        @DisplayName("addImage appends an image to the list")
        void addImage_appends() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = StoreProduct.builder()
                    .id(productId)
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .images(new ArrayList<>())
                    .build();
            ProductImage image = ProductImage.builder()
                    .productId(productId)
                    .imageUrl("http://example.com/img.jpg")
                    .displayOrder(1)
                    .build();
            product.addImage(image);
            assertThat(product.getImages()).containsExactly(image);
        }
    }

    @Nested
    @DisplayName("price and SKU helpers")
    class PriceAndSkuHelpers {

        private StoreProduct productWithSkus() {
            ProductId productId = ProductId.newProductId();
            List<ProductSku> skus = List.of(
                    buildSku(productId, new BigDecimal("10.00"), "USD"),
                    buildSku(productId, new BigDecimal("30.00"), "USD"),
                    buildSku(productId, new BigDecimal("20.00"), "USD")
            );
            return StoreProduct.builder()
                    .id(productId)
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .skus(new ArrayList<>(skus))
                    .build();
        }

        @Test
        @DisplayName("getMinPrice returns the lowest SKU price")
        void getMinPrice_returnsLowest() {
            assertThat(productWithSkus().getMinPrice()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("getMaxPrice returns the highest SKU price")
        void getMaxPrice_returnsHighest() {
            assertThat(productWithSkus().getMaxPrice()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("getMinPrice returns ZERO when skus list is null")
        void getMinPrice_returnsZero_whenNoSkus() {
            assertThat(buildProduct().getMinPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getCurrency returns the currency of the first SKU")
        void getCurrency_returnsFirstSkuCurrency() {
            assertThat(productWithSkus().getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("getCurrency returns null when skus is null")
        void getCurrency_returnsNull_whenNoSkus() {
            assertThat(buildProduct().getCurrency()).isNull();
        }

        @Test
        @DisplayName("getSkuCount returns the number of SKUs")
        void getSkuCount_returnsCorrectCount() {
            assertThat(productWithSkus().getSkuCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("getListOfSKUs extracts SKU identifiers from all SKUs")
        void getListOfSKUs_extractsSkuValues() {
            assertThat(productWithSkus().getListOfSKUs()).hasSize(3);
        }
    }
}
