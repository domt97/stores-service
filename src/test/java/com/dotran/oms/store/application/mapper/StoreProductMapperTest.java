package com.dotran.oms.store.application.mapper;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.command.storeproduct.CreateStoreProductCmd;
import com.dotran.oms.store.application.command.storeproduct.ProductSkuCmd;
import com.dotran.oms.store.common.id.ProductSkuId;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.domain.model.StoreProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StoreProductMapper")
class StoreProductMapperTest {

    private final StoreProductMapper mapper = new StoreProductMapperImpl();

    private CreateStoreProductCmd cmdWithoutImages() {
        return CreateStoreProductCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(StoreId.newStoreId())
                .name("Product")
                .skus(List.of(ProductSkuCmd.builder()
                        .sku(SKU.of("SKU-001"))
                        .price(new BigDecimal("25000"))
                        .currency("VND")
                        .build()))
                .build();
    }

    @Test
    @DisplayName("fromCreateStoreProductCmd maps a command without images to an empty image list")
    void fromCreateStoreProductCmd_withoutImages() {
        StoreProduct product = mapper.fromCreateStoreProductCmd(cmdWithoutImages());

        assertThat(product.getImages()).isNotNull().isEmpty();
        assertThat(product.getSkus()).hasSize(1);
    }

    @Test
    @DisplayName("a mapped product accepts addSku after initState (lists are mutable)")
    void mappedProduct_acceptsAddSku_afterInitState() {
        StoreProduct product = mapper.fromCreateStoreProductCmd(cmdWithoutImages());
        product.initState();

        product.addSku(ProductSku.builder()
                .id(ProductSkuId.newProductSkuId())
                .sku(SKU.of("SKU-002"))
                .price(BigDecimal.TEN)
                .currency("VND")
                .build());

        assertThat(product.getSkus()).hasSize(2);
    }

    @Test
    @DisplayName("fromProductImageCmds returns an empty list for null")
    void fromProductImageCmds_returnsEmpty_whenNull() {
        assertThat(mapper.fromProductImageCmds(null)).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("fromProductSkuCmds returns an empty list for null")
    void fromProductSkuCmds_returnsEmpty_whenNull() {
        assertThat(mapper.fromProductSkuCmds(null)).isNotNull().isEmpty();
    }
}
