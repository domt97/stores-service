package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.common.id.ProductImageId;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.exception.ProductRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoreProduct.replace (update-product)")
class StoreProductReplaceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private final ProductId productId = ProductId.newProductId();
    private final ProductSkuId idA = ProductSkuId.newProductSkuId();
    private final ProductSkuId idB = ProductSkuId.newProductSkuId();

    /** As loaded from the database: ACTIVE, SKUs A (100.00 VND) and B (200.00 VND), two images. */
    private StoreProduct stored() {
        return stored(ProductStatus.ACTIVE);
    }

    private StoreProduct stored(ProductStatus status) {
        return StoreProduct.builder()
                .id(productId)
                .storeId(StoreId.newStoreId())
                .name("Milk Tea")
                .description("Fresh")
                .status(status)
                .version(3)
                .skus(new ArrayList<>(List.of(storedSku(idA, "A", "100.00", null), storedSku(idB, "B", "200.00", null))))
                .images(new ArrayList<>(List.of(image("https://img/1.jpg", 1), image("https://img/2.jpg", 2))))
                .build();
    }

    private ProductSku storedSku(ProductSkuId id, String code, String price, Instant removedAt) {
        return ProductSku.builder()
                .id(id).productId(productId).sku(SKU.of(code)).name("Size " + code)
                .price(new BigDecimal(price)).currency("VND").weight(new BigDecimal("250.00"))
                .status(ProductSkuStatus.ACTIVE).removedAt(removedAt)
                .build();
    }

    private ProductImage image(String url, int order) {
        return ProductImage.builder().id(ProductImageId.of((long) order)).productId(productId).imageUrl(url).displayOrder(order).build();
    }

    private static ProductSkuSpec spec(String code, String price, ProductSkuStatus status) {
        return spec(null, code, price, "VND", status);
    }

    private static ProductSkuSpec spec(ProductSkuId id, String code, String price, String currency, ProductSkuStatus status) {
        return new ProductSkuSpec(id, SKU.of(code), "Size " + code, new BigDecimal(price), currency,
                new BigDecimal("250"), null, null, null, status);
    }

    /** The stored state as a request: no change. */
    private ProductReplacement same(List<ProductSkuSpec> skus) {
        return new ProductReplacement("Milk Tea", "Fresh", null, null, ProductStatus.ACTIVE, skus,
                List.of(new ProductReplacement.ImageSpec("https://img/1.jpg", 1), new ProductReplacement.ImageSpec("https://img/2.jpg", 2)));
    }

    private ProductReplacement with(ProductStatus status, List<ProductSkuSpec> skus) {
        ProductReplacement base = same(skus);
        return new ProductReplacement(base.name(), base.description(), null, null, status, skus, base.images());
    }

    @Nested
    @DisplayName("no change")
    class NoChange {

        @Test
        @DisplayName("an identical request is a no-op: 100 vs 100.00, lower-case currency, shuffled images and SKUs")
        void identicalRequest_isNoOp() {
            StoreProduct product = stored();
            ProductReplacement request = new ProductReplacement("Milk Tea", "Fresh", null, null, ProductStatus.ACTIVE,
                    List.of(spec(null, "B", "200", "vnd", ProductSkuStatus.ACTIVE), spec(null, "A", "100", " VND ", ProductSkuStatus.ACTIVE)),
                    List.of(new ProductReplacement.ImageSpec("https://img/2.jpg", 2), new ProductReplacement.ImageSpec("https://img/1.jpg", 1)));

            ProductChangeSet changes = product.replace(request, NOW);

            assertThat(changes.isEmpty()).isTrue();
            assertThat(product.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("every SKU in the body passes the price rule: a legacy lower-case currency is stored normalised")
        void legacyLowerCaseCurrency_normalised() {
            StoreProduct product = stored();
            ProductSku legacy = ProductSku.builder().id(idA).productId(productId).sku(SKU.of("A")).name("Size A")
                    .price(new BigDecimal("100.00")).currency("vnd").weight(new BigDecimal("250.00")).status(ProductSkuStatus.ACTIVE).build();
            product.getAllSkusIncludingRemoved().set(0, legacy);

            ProductChangeSet changes = product.replace(same(List.of(
                    spec(null, "A", "100", "vnd", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.ACTIVE))), NOW);

            assertThat(changes.updated()).extracting(sku -> sku.getSku().getValue()).containsExactly("A");
            assertThat(legacy.getCurrency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("a legacy SKU that breaks the price rule must be corrected when the product is edited (400)")
        void legacyInvalidSku_mustBeCorrected() {
            StoreProduct product = stored();
            product.getAllSkusIncludingRemoved().set(0, storedSku(idA, "A", "100.50", null));

            assertThatThrownBy(() -> product.replace(same(List.of(
                    spec("A", "100.50", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("SKU changes")
    class SkuChanges {

        @Test
        @DisplayName("a changed SKU is updated in place and keeps its id")
        void updatedSku_keepsId() {
            StoreProduct product = stored();

            ProductChangeSet changes = product.replace(same(List.of(
                    spec("A", "120", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.INACTIVE))), NOW);

            assertThat(changes.updated()).extracting(sku -> sku.getId()).containsExactlyInAnyOrder(idA, idB);
            assertThat(product.getSkus()).filteredOn(sku -> sku.getId().equals(idA)).singleElement()
                    .satisfies(sku -> assertThat(sku.getPrice()).isEqualByComparingTo("120"));
            assertThat(changes.added()).isEmpty();
            assertThat(changes.removed()).isEmpty();
        }

        @Test
        @DisplayName("a new code is added (no id yet), normalised by the price rule")
        void newCode_isAdded() {
            StoreProduct product = stored();

            ProductChangeSet changes = product.replace(same(List.of(spec("A", "100", ProductSkuStatus.ACTIVE),
                    spec("B", "200", ProductSkuStatus.ACTIVE), spec(null, "C", "9.99", "usd", ProductSkuStatus.ACTIVE))), NOW);

            assertThat(changes.added()).singleElement().satisfies(sku -> {
                assertThat(sku.getId()).isNull();
                assertThat(sku.getSku().getValue()).isEqualTo("C");
                assertThat(sku.getCurrency()).isEqualTo("USD");
                assertThat(sku.getProductId()).isEqualTo(productId);
                assertThat(sku.getCreatedAt()).isEqualTo(NOW);
            });
            assertThat(product.getSkus()).hasSize(3);
        }

        @Test
        @DisplayName("a code left out is removed: hidden from getSkus, prices, count and ProductCreated codes")
        void missingCode_isRemoved() {
            StoreProduct product = stored();

            ProductChangeSet changes = product.replace(same(List.of(spec("A", "100", ProductSkuStatus.ACTIVE))), NOW);

            assertThat(changes.removed()).extracting(ProductSku::getId).containsExactly(idB);
            assertThat(product.getSkus()).extracting(ProductSku::getId).containsExactly(idA);
            assertThat(product.getAllSkusIncludingRemoved()).hasSize(2);
            assertThat(product.getMaxPrice()).isEqualByComparingTo("100");
            assertThat(product.getSkuCount()).isEqualTo(1);
            assertThat(product.getListOfSKUs()).extracting(SKU::getValue).containsExactly("A");
            assertThat(changes.removed().get(0).getRemovedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("a removed code sent again is revived: same id, removedAt cleared")
        void removedCode_isRevived() {
            StoreProduct product = stored();
            product.getAllSkusIncludingRemoved().set(1, storedSku(idB, "B", "200.00", Instant.parse("2026-10-01T00:00:00Z")));

            ProductChangeSet changes = product.replace(same(List.of(
                    spec("A", "100", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.ACTIVE))), NOW);

            assertThat(changes.updated()).singleElement().satisfies(sku -> {
                assertThat(sku.getId()).isEqualTo(idB);
                assertThat(sku.getRemovedAt()).isNull();
            });
            assertThat(changes.added()).isEmpty();
            assertThat(product.getSkus()).hasSize(2);
        }

        @Test
        @DisplayName("SKU codes are case-sensitive: \"a\" is a new SKU, \"A\" is removed")
        void codesAreCaseSensitive() {
            StoreProduct product = stored();

            ProductChangeSet changes = product.replace(same(List.of(
                    spec("a", "100", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.ACTIVE))), NOW);

            assertThat(changes.added()).extracting(sku -> sku.getSku().getValue()).containsExactly("a");
            assertThat(changes.removed()).extracting(ProductSku::getId).containsExactly(idA);
        }
    }

    @Nested
    @DisplayName("product fields and images")
    class Fields {

        @Test
        @DisplayName("omitted optional fields are cleared (full replace)")
        void omittedFields_cleared() {
            StoreProduct product = stored();
            ProductReplacement request = new ProductReplacement("Milk Tea", null, null, null, ProductStatus.ACTIVE,
                    List.of(spec("A", "100", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.ACTIVE)), same(List.of()).images());

            ProductChangeSet changes = product.replace(request, NOW);

            assertThat(changes.productFieldsChanged()).isTrue();
            assertThat(changes.imagesChanged()).isFalse();
            assertThat(product.getDescription()).isNull();
            assertThat(product.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("a different image list replaces the images, in display order")
        void imagesReplaced() {
            StoreProduct product = stored();
            ProductReplacement request = new ProductReplacement("Milk Tea", "Fresh", null, null, ProductStatus.ACTIVE,
                    List.of(spec("A", "100", ProductSkuStatus.ACTIVE), spec("B", "200", ProductSkuStatus.ACTIVE)),
                    List.of(new ProductReplacement.ImageSpec("https://img/new.jpg", 2), new ProductReplacement.ImageSpec("https://img/1.jpg", 1)));

            ProductChangeSet changes = product.replace(request, NOW);

            assertThat(changes.imagesChanged()).isTrue();
            assertThat(changes.productFieldsChanged()).isFalse();
            assertThat(product.getImages()).extracting(ProductImage::getImageUrl).containsExactly("https://img/1.jpg", "https://img/new.jpg");
        }
    }

    @Nested
    @DisplayName("status (BR-CAT-10)")
    class Status {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"ACTIVE, ACTIVE", "ACTIVE, INACTIVE", "INACTIVE, ACTIVE", "INACTIVE, INACTIVE",
                "DRAFT, DRAFT", "DRAFT, ACTIVE", "DRAFT, INACTIVE"})
        @DisplayName("allowed changes")
        void allowed(ProductStatus from, ProductStatus to) {
            StoreProduct product = stored(from);

            product.replace(with(to, List.of(spec("A", "100", ProductSkuStatus.ACTIVE))), NOW);

            assertThat(product.getStatus()).isEqualTo(to);
        }

        @ParameterizedTest(name = "{0} → DRAFT")
        @CsvSource({"ACTIVE", "INACTIVE"})
        @DisplayName("never back to DRAFT → 422")
        void backToDraft_rejected(ProductStatus from) {
            StoreProduct product = stored(from);

            assertThatThrownBy(() -> product.replace(with(ProductStatus.DRAFT, List.of(spec("A", "100", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ProductRuleViolationException.class);
        }

        @Test
        @DisplayName("ACTIVE with every SKU INACTIVE → 422, product unchanged")
        void activeWithoutActiveSku_rejected() {
            StoreProduct product = stored();

            assertThatThrownBy(() -> product.replace(with(ProductStatus.ACTIVE, List.of(
                    spec("A", "100", ProductSkuStatus.INACTIVE), spec("B", "200", ProductSkuStatus.INACTIVE))), NOW))
                    .isInstanceOf(ProductRuleViolationException.class);
            assertThat(product.getSkus()).allMatch(sku -> sku.getStatus() == ProductSkuStatus.ACTIVE);
        }

        @Test
        @DisplayName("INACTIVE with every SKU INACTIVE is allowed")
        void inactiveWithoutActiveSku_allowed() {
            StoreProduct product = stored();

            product.replace(with(ProductStatus.INACTIVE, List.of(spec("A", "100", ProductSkuStatus.INACTIVE))), NOW);

            assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("invalid requests (400)")
    class Invalid {

        @Test
        @DisplayName("a SKU id that is not the id of the stored SKU with that code")
        void idMismatch() {
            StoreProduct product = stored();

            assertThatThrownBy(() -> product.replace(same(List.of(spec(idB, "A", "100", "VND", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("an id sent for a new code")
        void idForNewCode() {
            StoreProduct product = stored();

            assertThatThrownBy(() -> product.replace(same(List.of(spec(ProductSkuId.newProductSkuId(), "NEW", "100", "VND", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("the matching id is accepted")
        void matchingId_accepted() {
            StoreProduct product = stored();

            assertThat(product.replace(same(List.of(spec(idA, "A", "100", "VND", ProductSkuStatus.ACTIVE),
                    spec(idB, "B", "200", "VND", ProductSkuStatus.ACTIVE))), NOW).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("a duplicate code, a blank code, or no SKU")
        void badCodes() {
            assertThatThrownBy(() -> stored().replace(same(List.of(spec("A", "100", ProductSkuStatus.ACTIVE), spec("A", "100", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> stored().replace(same(List.of(spec(" ", "100", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> stored().replace(same(List.of()), NOW))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("a blank name or a SKU without status (requests that bypass REST validation)")
        void missingNameOrSkuStatus() {
            assertThatThrownBy(() -> stored().replace(new ProductReplacement(" ", null, null, null, ProductStatus.ACTIVE,
                    List.of(spec("A", "100", ProductSkuStatus.ACTIVE)), List.of()), NOW)).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> stored().replace(same(List.of(spec("A", "100", null))), NOW))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("over the limits: 101 SKUs, 21 images, 5001-character description")
        void overLimits() {
            List<ProductSkuSpec> manySkus = IntStream.range(0, 101).mapToObj(i -> spec("S" + i, "1", ProductSkuStatus.ACTIVE)).toList();
            assertThatThrownBy(() -> stored().replace(same(manySkus), NOW)).isInstanceOf(ValidationException.class);

            List<ProductReplacement.ImageSpec> manyImages = IntStream.range(0, 21)
                    .mapToObj(i -> new ProductReplacement.ImageSpec("https://img/" + i, i)).toList();
            assertThatThrownBy(() -> stored().replace(new ProductReplacement("Milk Tea", "Fresh", null, null, ProductStatus.ACTIVE,
                    List.of(spec("A", "100", ProductSkuStatus.ACTIVE)), manyImages), NOW)).isInstanceOf(ValidationException.class);

            assertThatThrownBy(() -> stored().replace(new ProductReplacement("Milk Tea", "x".repeat(5001), null, null, ProductStatus.ACTIVE,
                    List.of(spec("A", "100", ProductSkuStatus.ACTIVE)), List.of()), NOW)).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("100 SKUs and 20 images are accepted")
        void atLimits_accepted() {
            List<ProductSkuSpec> skus = IntStream.range(0, 100).mapToObj(i -> spec("S" + i, "1", ProductSkuStatus.ACTIVE)).toList();
            List<ProductReplacement.ImageSpec> images = IntStream.range(0, 20)
                    .mapToObj(i -> new ProductReplacement.ImageSpec("https://img/" + i, i)).toList();

            ProductChangeSet changes = stored().replace(new ProductReplacement("Milk Tea", "Fresh", null, null, ProductStatus.ACTIVE, skus, images), NOW);

            assertThat(changes.added()).hasSize(100);
        }

        @Test
        @DisplayName("the SKU price rule applies to added and changed SKUs: a fractional VND price")
        void priceRule() {
            assertThatThrownBy(() -> stored().replace(same(List.of(spec("A", "100.5", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> stored().replace(same(List.of(spec("A", "100", ProductSkuStatus.ACTIVE), spec("NEW", "0.5", ProductSkuStatus.ACTIVE))), NOW))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Test
    @DisplayName("addSku rejects a code the product already has, removed SKUs included")
    void addSku_rejectsDuplicateCode() {
        StoreProduct product = stored();
        product.getAllSkusIncludingRemoved().set(1, storedSku(idB, "B", "200.00", NOW));

        assertThatThrownBy(() -> product.addSku(storedSku(ProductSkuId.newProductSkuId(), "B", "1", null)))
                .isInstanceOf(ValidationException.class);
    }
}
