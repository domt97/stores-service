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
    @DisplayName("canAddProductsToCollection")
    class CanAddProducts {

        @Test
        @DisplayName("returns true for a valid list of new products")
        void returnsTrue_forValidProducts() {
            StoreCollection collection = emptyCollection();
            assertThat(collection.canAddProductsToCollection(List.of(productA))).isTrue();
        }

        @Test
        @DisplayName("throws BusinessException for a null product list")
        void throwsException_forNullList() {
            StoreCollection collection = emptyCollection();
            assertThatThrownBy(() -> collection.canAddProductsToCollection(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws BusinessException for an empty product list")
        void throwsException_forEmptyList() {
            StoreCollection collection = emptyCollection();
            assertThatThrownBy(() -> collection.canAddProductsToCollection(List.of()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws BusinessException when a product already exists in the collection")
        void throwsException_forDuplicateProduct() {
            StoreCollection collection = collectionWith(productA);
            assertThatThrownBy(() -> collection.canAddProductsToCollection(List.of(productA)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("canRemoveProductsFromCollection")
    class CanRemoveProducts {

        @Test
        @DisplayName("returns true when all specified products exist in the collection")
        void returnsTrue_forExistingProducts() {
            StoreCollection collection = collectionWith(productA, productB);
            assertThat(collection.canRemoveProductsFromCollection(List.of(productA))).isTrue();
        }

        @Test
        @DisplayName("throws BusinessException for a null product list")
        void throwsException_forNullList() {
            StoreCollection collection = collectionWith(productA);
            assertThatThrownBy(() -> collection.canRemoveProductsFromCollection(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws BusinessException for an empty product list")
        void throwsException_forEmptyList() {
            StoreCollection collection = collectionWith(productA);
            assertThatThrownBy(() -> collection.canRemoveProductsFromCollection(List.of()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws BusinessException when a product does not exist in the collection")
        void throwsException_forNonExistingProduct() {
            StoreCollection collection = collectionWith(productA);
            ProductId notInCollection = ProductId.newProductId();
            assertThatThrownBy(() -> collection.canRemoveProductsFromCollection(List.of(notInCollection)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not exist");
        }
    }

    @Nested
    @DisplayName("removeProduct")
    class RemoveProduct {

        @Test
        @DisplayName("removes the specified products from the collection")
        void removesProducts() {
            StoreCollection collection = collectionWith(productA, productB);
            collection.removeProduct(List.of(productA));
            assertThat(collection.getProductIds()).containsExactly(productB);
        }

        @Test
        @DisplayName("leaves other products intact after removal")
        void leavesOtherProductsIntact() {
            StoreCollection collection = collectionWith(productA, productB);
            collection.removeProduct(productA);
            assertThat(collection.getProductIds()).containsExactly(productB);
        }
    }
}
