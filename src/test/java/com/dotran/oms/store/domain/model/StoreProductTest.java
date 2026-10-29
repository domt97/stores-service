package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.exception.ProductRuleViolationException;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoreProduct")
class StoreProductTest {

    private int skuSequence;

    /** A product with one ACTIVE SKU, ready for initState (an ACTIVE product needs an ACTIVE SKU, BR-CAT-10). */
    private StoreProduct buildProduct() {
        ProductId productId = ProductId.newProductId();
        return StoreProduct.builder()
                .id(productId)
                .storeId(StoreId.newStoreId())
                .name("Test Product")
                .skus(new ArrayList<>(List.of(buildSku(productId, BigDecimal.TEN, "VND"))))
                .build();
    }

    private StoreProduct productWithoutSkus() {
        return StoreProduct.builder().id(ProductId.newProductId()).storeId(StoreId.newStoreId()).name("No SKUs").build();
    }

    /** ACTIVE SKU with a new code per call: SKU-001, SKU-002, … */
    private ProductSku buildSku(ProductId productId, BigDecimal price, String currency) {
        return ProductSku.builder()
                .id(ProductSkuId.newProductSkuId())
                .productId(productId)
                .sku(SKU.of(String.format("SKU-%03d", ++skuSequence)))
                .name("Variant")
                .price(price)
                .currency(currency)
                .status(ProductSkuStatus.ACTIVE)
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
        @DisplayName("rejects a product without SKUs: an ACTIVE product needs an ACTIVE SKU")
        void rejectsProductWithoutSkus() {
            StoreProduct product = StoreProduct.builder()
                    .id(ProductId.newProductId())
                    .storeId(StoreId.newStoreId())
                    .name("No SKUs")
                    .build();

            assertThatThrownBy(product::initState).isInstanceOf(ProductRuleViolationException.class);
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
            ProductSku sku = buildSku(productId, BigDecimal.TEN, "VND");
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
            ProductSku sku = buildSku(productId, BigDecimal.TEN, "VND");
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
                    buildSku(productId, new BigDecimal("10.00"), "VND"),
                    buildSku(productId, new BigDecimal("30.00"), "VND"),
                    buildSku(productId, new BigDecimal("20.00"), "VND")
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
            assertThat(productWithoutSkus().getMinPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getCurrency returns the currency of the first SKU")
        void getCurrency_returnsFirstSkuCurrency() {
            assertThat(productWithSkus().getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("getCurrency returns null when skus is null")
        void getCurrency_returnsNull_whenNoSkus() {
            assertThat(productWithoutSkus().getCurrency()).isNull();
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

    @Nested
    @DisplayName("sale rule (3-letter currency, price ≥ 0 with ≤ 2 decimals, whole VND)")
    class SaleRule {

        private StoreProduct productWith(ProductSku... skus) {
            return StoreProduct.builder()
                    .id(ProductId.newProductId())
                    .storeId(StoreId.newStoreId())
                    .name("Product")
                    .skus(new ArrayList<>(List.of(skus)))
                    .build();
        }

        @Test
        @DisplayName("initState accepts VND with a whole price, including 10.00 and 0")
        void initState_acceptsWholeVnd() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = productWith(
                    buildSku(productId, new BigDecimal("10.00"), "VND"),
                    buildSku(productId, BigDecimal.ZERO, "VND"),
                    buildSku(productId, new BigDecimal("25000"), "VND"));

            product.initState();

            assertThat(product.getSkus()).extracting(ProductSku::getCurrency).containsOnly("VND");
        }

        @Test
        @DisplayName("initState accepts another currency with up to 2 decimals")
        void initState_acceptsOtherCurrency() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = productWith(
                    buildSku(productId, new BigDecimal("9.99"), "USD"),
                    buildSku(productId, new BigDecimal("12.5"), "EUR"));

            product.initState();

            assertThat(product.getSkus()).extracting(ProductSku::getCurrency).containsExactly("USD", "EUR");
        }

        @Test
        @DisplayName("initState normalises the currency code to upper case without surrounding spaces")
        void initState_normalisesCurrency() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = productWith(
                    buildSku(productId, BigDecimal.TEN, "vnd"),
                    buildSku(productId, BigDecimal.TEN, " usd "));

            product.initState();

            assertThat(product.getSkus()).extracting(ProductSku::getCurrency).containsExactly("VND", "USD");
        }

        @Test
        @DisplayName("initState rejects a currency that is not a 3-letter code, or null, without a NullPointerException")
        void initState_rejectsInvalidCurrency() {
            for (String currency : new String[]{"US", "DOLLAR", "U5D", "", null}) {
                StoreProduct product = productWith(buildSku(ProductId.newProductId(), BigDecimal.TEN, currency));

                assertThatThrownBy(product::initState)
                        .as("currency %s", currency)
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("SKU-");
            }
        }

        @Test
        @DisplayName("initState rejects a fractional VND price")
        void initState_rejectsFractionalVnd() {
            StoreProduct product = productWith(buildSku(ProductId.newProductId(), new BigDecimal("10.5"), "VND"));

            assertThatThrownBy(product::initState)
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VND");
        }

        @Test
        @DisplayName("initState rejects a negative, missing or too large price, and more than 2 decimals in any currency")
        void initState_rejectsInvalidPrice() {
            Object[][] cases = {{new BigDecimal("-1"), "VND"}, {null, "VND"}, {new BigDecimal("9.999"), "USD"}, {new BigDecimal("-0.01"), "USD"}, {new BigDecimal("1E16"), "VND"}};
            for (Object[] c : cases) {
                StoreProduct product = productWith(buildSku(ProductId.newProductId(), (BigDecimal) c[0], (String) c[1]));

                assertThatThrownBy(product::initState)
                        .as("price %s %s", c[0], c[1])
                        .isInstanceOf(ValidationException.class);
            }
        }

        @Test
        @DisplayName("initState rejects the product when one of several SKUs breaks the rule")
        void initState_rejectsMixedSkus() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = productWith(
                    buildSku(productId, BigDecimal.TEN, "VND"),
                    buildSku(productId, new BigDecimal("10.5"), "VND"));

            assertThatThrownBy(product::initState).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("addSku rejects an invalid SKU and leaves the SKU list unchanged")
        void addSku_rejectsInvalidSku() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = productWith(buildSku(productId, BigDecimal.TEN, "VND"));

            assertThatThrownBy(() -> product.addSku(buildSku(productId, new BigDecimal("10.5"), "VND")))
                    .isInstanceOf(ValidationException.class);

            assertThat(product.getSkus()).hasSize(1);
        }

        @Test
        @DisplayName("addSku normalises the currency of the added SKU")
        void addSku_normalisesCurrency() {
            ProductId productId = ProductId.newProductId();
            StoreProduct product = productWith();

            product.addSku(buildSku(productId, BigDecimal.TEN, "usd"));

            assertThat(product.getSkus()).extracting(ProductSku::getCurrency).containsExactly("USD");
        }
    }
}
