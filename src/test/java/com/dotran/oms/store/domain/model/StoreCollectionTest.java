package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.enums.CollectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoreCollection")
class StoreCollectionTest {

    private ProductId productA;
    private ProductId productB;

    @BeforeEach
    void setUp() {
        productA = ProductId.newProductId();
        productB = ProductId.newProductId();
    }

    private StoreCollection emptyCollection() {
        return StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(StoreId.newStoreId())
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>())
                .build();
    }

    private StoreCollection collectionWith(ProductId... ids) {
        return StoreCollection.builder()
                .id(StoreCollectionId.generateId())
                .storeId(StoreId.newStoreId())
                .name("Test Collection")
                .status(CollectionStatus.ACTIVE)
                .productIds(new ArrayList<>(List.of(ids)))
                .build();
    }

    @Nested
    @DisplayName("addProduct")
    class AddProduct {

        @Test
        @DisplayName("adds a valid product to the collection")
        void addsProduct() {
            StoreCollection collection = emptyCollection();
            collection.addProduct(productA);
            assertThat(collection.getProductIds()).containsExactly(productA);
        }

        @Test
        @DisplayName("ignores null without throwing")
        void ignoresNull() {
            StoreCollection collection = emptyCollection();
            collection.addProduct(null);
            assertThat(collection.getProductIds()).isEmpty();
        }

        @Test
        @DisplayName("updates updatedAt when product is added")
        void updatesTimestamp() {
            StoreCollection collection = emptyCollection();
            collection.addProduct(productA);
            assertThat(collection.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("checkProductListChange (BR-CAT-15: add/remove are idempotent, only an empty request is refused)")
    class CheckProductListChange {

        @Test
        @DisplayName("throws BusinessException for a null or empty product list")
        void throwsException_forNullOrEmptyList() {
            assertThatThrownBy(() -> StoreCollection.checkProductListChange(null)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> StoreCollection.checkProductListChange(List.of())).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("accepts any non-empty list: products already in the collection (or not in it) are not an error")
        void acceptsNonEmptyList() {
            StoreCollection.checkProductListChange(List.of(productA, productB));
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("sets status to ACTIVE")
        void setsStatusActive() {
            StoreCollection collection = StoreCollection.builder()
                    .id(StoreCollectionId.generateId())
                    .storeId(StoreId.newStoreId())
                    .name("New Collection")
                    .productIds(new ArrayList<>())
                    .build();
            collection.init();
            assertThat(collection.getStatus()).isEqualTo(CollectionStatus.ACTIVE);
        }

        @Test
        @DisplayName("sets createdAt and updatedAt")
        void setsTimestamps() {
            StoreCollection collection = StoreCollection.builder()
                    .id(StoreCollectionId.generateId())
                    .storeId(StoreId.newStoreId())
                    .name("New Collection")
                    .productIds(new ArrayList<>())
                    .build();
            collection.init();
            assertThat(collection.getCreatedAt()).isNotNull();
            assertThat(collection.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class StatusTransitions {

        @Test
        @DisplayName("activate sets status to ACTIVE")
        void activate_setsStatusActive() {
            StoreCollection collection = StoreCollection.builder()
                    .id(StoreCollectionId.generateId())
                    .storeId(StoreId.newStoreId())
                    .name("Test")
                    .status(CollectionStatus.INACTIVE)
                    .productIds(new ArrayList<>())
                    .build();
            collection.activate();
            assertThat(collection.getStatus()).isEqualTo(CollectionStatus.ACTIVE);
        }

        @Test
        @DisplayName("deactivate sets status to INACTIVE")
        void deactivate_setsStatusInactive() {
            StoreCollection collection = emptyCollection();
            collection.deactivate();
            assertThat(collection.getStatus()).isEqualTo(CollectionStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("getProductCount")
    class GetProductCount {

        @Test
        @DisplayName("returns the number of products in the collection")
        void returnsCorrectCount() {
            StoreCollection collection = collectionWith(productA, productB);
            assertThat(collection.getProductCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("returns 0 when collection has no products")
        void returnsZero_whenEmpty() {
            StoreCollection collection = emptyCollection();
            assertThat(collection.getProductCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("returns 0 when productIds is null")
        void returnsZero_whenNull() {
            StoreCollection collection = StoreCollection.builder()
                    .id(StoreCollectionId.generateId())
                    .storeId(StoreId.newStoreId())
                    .name("Test")
                    .productIds(null)
                    .build();
            assertThat(collection.getProductCount()).isEqualTo(0);
        }
    }
}
