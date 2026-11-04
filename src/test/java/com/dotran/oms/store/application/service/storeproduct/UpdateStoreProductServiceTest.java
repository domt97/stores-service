package com.dotran.oms.store.application.service.storeproduct;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.application.command.storeproduct.UpdateProductSkuCmd;
import com.dotran.oms.store.application.command.storeproduct.UpdateStoreProductCmd;
import com.dotran.oms.store.application.dto.StoreProductDetailDto;
import com.dotran.oms.store.application.event.OutboxEventHelper;
import com.dotran.oms.store.application.mapper.StoreProductMapper;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.OutboxEventRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.event.OutboxEvent;
import com.dotran.oms.store.domain.exception.ProductRuleViolationException;
import com.dotran.oms.store.domain.exception.ProductVersionConflictException;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.model.ProductChangeSet;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateStoreProductService")
class UpdateStoreProductServiceTest {

    @Mock private StoreProductRepository repository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private StoreProductMapper mapper;
    @Mock private OutboxEventHelper outboxEventHelper;

    private UpdateStoreProductService service;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    /** The store's owner as loaded; deliberately a different object from the path merchant id. */
    private final MerchantId storeOwner = MerchantId.of(merchantId.getValue());
    private final StoreId storeId = StoreId.newStoreId();
    private final ProductId productId = ProductId.newProductId();
    private final ProductSkuId skuId = ProductSkuId.newProductSkuId();
    private Store store;

    @BeforeEach
    void setUp() {
        service = new UpdateStoreProductService(repository, merchantRepository, storeRepository, outboxEventRepository,
                mapper, outboxEventHelper);
        store = Store.builder().id(storeId).merchantId(storeOwner).build();
    }

    private StoreProduct storedProduct(long version) {
        ProductSku sku = ProductSku.builder().id(skuId).productId(productId).sku(SKU.of("A")).name("Size A")
                .price(new BigDecimal("100.00")).currency("VND").status(ProductSkuStatus.ACTIVE).build();
        return StoreProduct.builder().id(productId).storeId(storeId).name("Milk Tea").status(ProductStatus.ACTIVE)
                .version(version).skus(new ArrayList<>(List.of(sku))).images(new ArrayList<>()).build();
    }

    private UpdateStoreProductCmd cmd(long version, String name, List<UpdateProductSkuCmd> skus) {
        return UpdateStoreProductCmd.builder()
                .merchantId(merchantId).storeId(storeId).productId(productId).version(version)
                .name(name).status(ProductStatus.ACTIVE).skus(skus).images(List.of())
                .build();
    }

    private static UpdateProductSkuCmd sku(String code, String price, ProductSkuStatus status) {
        return UpdateProductSkuCmd.builder().sku(SKU.of(code)).name("Size " + code).price(new BigDecimal(price))
                .currency("VND").status(status).build();
    }

    private void found(StoreProduct product) {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.of(store));
        when(repository.getByStoreIdAndProductId(storeId, productId)).thenReturn(Optional.of(product));
    }

    private void verifyNothingWritten() {
        verify(repository, never()).bumpVersion(any(), any(), anyLong(), any());
        verify(repository, never()).applyChanges(any(), any(), anyLong());
        verifyNoInteractions(outboxEventRepository, outboxEventHelper);
    }

    @Test
    @DisplayName("404 when the merchant does not exist, the store is not the merchant's, or the product is not in the store")
    void notFound() {
        when(merchantRepository.existsById(merchantId)).thenReturn(false);
        assertThatThrownBy(() -> service.updateProduct(cmd(3, "X", List.of(sku("A", "100", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(NotFoundException.class).hasMessage("Merchant not found");

        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProduct(cmd(3, "X", List.of(sku("A", "100", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(NotFoundException.class).hasMessage("Store not found");

        when(storeRepository.findByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(Optional.of(store));
        when(repository.getByStoreIdAndProductId(storeId, productId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProduct(cmd(3, "X", List.of(sku("A", "100", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(NotFoundException.class).hasMessage("Product not found");

        verifyNothingWritten();
    }

    @Test
    @DisplayName("an unchanged body returns 200 with the current product and writes nothing, even with a stale version")
    void noOp_evenWithStaleVersion() {
        StoreProduct product = storedProduct(5);
        found(product);
        StoreProductDetailDto dto = StoreProductDetailDto.builder().version(5).build();
        when(repository.currentVersion(storeId, productId)).thenReturn(5L);
        when(mapper.fromStoreProduct(product)).thenReturn(dto);

        StoreProductDetailDto result = service.updateProduct(cmd(4, "Milk Tea", List.of(sku("A", "100", ProductSkuStatus.ACTIVE))));

        assertThat(result.getVersion()).isEqualTo(5);
        verifyNothingWritten();
    }

    @Test
    @DisplayName("an unchanged body judged on a mixed read (another update committed meanwhile) → 409, nothing written")
    void noOp_onMixedRead_conflict() {
        found(storedProduct(5));
        when(repository.currentVersion(storeId, productId)).thenReturn(6L);

        assertThatThrownBy(() -> service.updateProduct(cmd(5, "Milk Tea", List.of(sku("A", "100", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(ProductVersionConflictException.class);

        verifyNothingWritten();
    }

    @Test
    @DisplayName("a changed body with a stale version → 409, nothing written")
    void staleVersion_conflict() {
        found(storedProduct(5));

        assertThatThrownBy(() -> service.updateProduct(cmd(4, "Bubble Tea", List.of(sku("A", "100", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(ProductVersionConflictException.class)
                .hasMessageContaining("current version 5");

        verifyNothingWritten();
    }

    @Test
    @DisplayName("a rule violation judged against a newer state than the client read → 409, not 422")
    void ruleViolationOnStaleState_conflict() {
        found(storedProduct(5));

        assertThatThrownBy(() -> service.updateProduct(cmd(4, "Milk Tea", List.of(sku("A", "100", ProductSkuStatus.INACTIVE)))))
                .isInstanceOf(ProductVersionConflictException.class);

        verifyNothingWritten();
    }

    @Test
    @DisplayName("a rule violation on the current version stays a rule violation (422) or validation error (400)")
    void ruleViolationOnCurrentVersion_kept() {
        found(storedProduct(5));
        assertThatThrownBy(() -> service.updateProduct(cmd(5, "Milk Tea", List.of(sku("A", "100", ProductSkuStatus.INACTIVE)))))
                .isInstanceOf(ProductRuleViolationException.class);

        assertThatThrownBy(() -> service.updateProduct(cmd(5, "Milk Tea", List.of(sku("A", "100.5", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(ValidationException.class);

        verifyNothingWritten();
    }

    @Test
    @DisplayName("the guarded bump losing to a concurrent update → 409, nothing applied")
    void bumpLost_conflict() {
        found(storedProduct(5));
        when(repository.bumpVersion(eq(storeId), eq(productId), eq(5L), any())).thenReturn(false);

        assertThatThrownBy(() -> service.updateProduct(cmd(5, "Bubble Tea", List.of(sku("A", "100", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(ProductVersionConflictException.class);

        verify(repository, never()).applyChanges(any(), any(), anyLong());
        verifyNoInteractions(outboxEventRepository, outboxEventHelper);
    }

    @Test
    @DisplayName("an added SKU code used by another product of the store → 409, nothing applied")
    void codeInUse_conflict() {
        found(storedProduct(5));
        when(repository.bumpVersion(eq(storeId), eq(productId), eq(5L), any())).thenReturn(true);
        when(repository.findSkuCodesOfOtherProducts(eq(storeId), eq(productId), anyCollection())).thenReturn(Set.of(SKU.of("B")));

        assertThatThrownBy(() -> service.updateProduct(cmd(5, "Milk Tea",
                List.of(sku("A", "100", ProductSkuStatus.ACTIVE), sku("B", "200", ProductSkuStatus.ACTIVE)))))
                .isInstanceOf(SkuCodeInUseException.class)
                .hasMessageContaining("B");

        verify(repository, never()).applyChanges(any(), any(), anyLong());
        verifyNoInteractions(outboxEventRepository, outboxEventHelper);
    }

    @Test
    @DisplayName("success: bump → code check → apply → outbox, ProductUpdated with version v+1 for the store's merchant")
    void success_orderAndEvent() {
        StoreProduct product = storedProduct(5);
        found(product);
        StoreProduct saved = storedProduct(6);
        OutboxEvent event = OutboxEvent.builder().eventType("ProductUpdated").build();
        StoreProductDetailDto dto = StoreProductDetailDto.builder().version(6).build();
        when(repository.bumpVersion(eq(storeId), eq(productId), eq(5L), any())).thenReturn(true);
        when(repository.findSkuCodesOfOtherProducts(eq(storeId), eq(productId), anyCollection())).thenReturn(Set.of());
        when(repository.applyChanges(eq(product), any(ProductChangeSet.class), eq(6L))).thenReturn(saved);
        when(outboxEventHelper.createProductUpdatedOutboxEvent(storeOwner, saved, 6L)).thenReturn(event);
        when(mapper.fromStoreProduct(saved)).thenReturn(dto);

        StoreProductDetailDto result = service.updateProduct(cmd(5, "Milk Tea",
                List.of(sku("A", "100", ProductSkuStatus.ACTIVE), sku("NEW", "300", ProductSkuStatus.ACTIVE))));

        assertThat(result.getVersion()).isEqualTo(6);
        InOrder order = inOrder(repository, outboxEventRepository);
        order.verify(repository).bumpVersion(eq(storeId), eq(productId), eq(5L), any());
        order.verify(repository).findSkuCodesOfOtherProducts(eq(storeId), eq(productId), anyCollection());
        order.verify(repository).applyChanges(eq(product), any(ProductChangeSet.class), eq(6L));
        order.verify(outboxEventRepository).save(event);

        ArgumentCaptor<ProductChangeSet> changes = ArgumentCaptor.forClass(ProductChangeSet.class);
        verify(repository).applyChanges(eq(product), changes.capture(), eq(6L));
        assertThat(changes.getValue().added()).extracting(added -> added.getSku().getValue()).containsExactly("NEW");
    }
}
