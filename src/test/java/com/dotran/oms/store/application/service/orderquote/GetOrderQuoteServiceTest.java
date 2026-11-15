package com.dotran.oms.store.application.service.orderquote;

import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.orderquote.GetOrderQuoteCmd;
import com.dotran.oms.store.application.dto.orderquote.OrderQuoteDto;
import com.dotran.oms.store.application.dto.orderquote.SkuQuoteRow;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import com.dotran.oms.store.domain.enums.OpenReason;
import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.model.BusinessHour;
import com.dotran.oms.store.domain.model.Merchant;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreAvailability;
import com.dotran.oms.store.domain.valueobject.StoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderQuoteService")
class GetOrderQuoteServiceTest {

    /** Monday 2026-11-16 10:00 in Ho Chi Minh City. */
    private static final Instant NOW = Instant.parse("2026-11-16T03:00:00Z");

    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreAvailabilityRepository storeAvailabilityRepository;
    @Mock private StoreProductRepository storeProductRepository;

    private GetOrderQuoteService service;
    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();

    @BeforeEach
    void setUp() {
        service = new GetOrderQuoteService(merchantRepository, storeRepository, storeAvailabilityRepository,
                storeProductRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Merchant merchant(MerchantStatus status) {
        return Merchant.builder().id(merchantId).status(status).build();
    }

    private Store store() {
        return Store.builder().id(storeId).merchantId(merchantId).name("Store One").code("S1")
                .ownerId(CustomerId.newCustomerId()).status(StoreStatus.ACTIVE)
                .config(StoreConfig.builder().timeZone("Asia/Ho_Chi_Minh").currency("VND").autoAcceptOrder(true)
                        .maxOrdersPerDay(50).preparationTimeMinutes(15).build())
                .businessHours(new ArrayList<>(List.of(BusinessHour.builder().dayOfWeek(DayOfWeek.MONDAY)
                        .openingTime(LocalTime.of(8, 0)).closingTime(LocalTime.of(18, 0)).build())))
                .build();
    }

    private GetOrderQuoteCmd cmd(String... skus) {
        return GetOrderQuoteCmd.builder().merchantId(merchantId).storeId(storeId).skus(Arrays.asList(skus)).build();
    }

    private void givenMerchantAndStore(MerchantStatus status) {
        when(merchantRepository.findById(eq(merchantId))).thenReturn(Optional.of(merchant(status)));
        when(storeRepository.findByMerchantIdAndStoreId(eq(merchantId), eq(storeId))).thenReturn(Optional.of(store()));
        // lenient: the window test replaces this answer
        lenient().when(storeAvailabilityRepository.findActiveCovering(eq(storeId), eq(NOW))).thenReturn(List.of());
    }

    private static SkuQuoteRow row(String sku) {
        return new SkuQuoteRow(sku, UUID.randomUUID(), "Product " + sku, "Size M", new BigDecimal("25000.00"), "VND",
                ProductSkuStatus.ACTIVE, ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("merchant missing → 404, nothing else read")
    void merchantMissing() {
        when(merchantRepository.findById(eq(merchantId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(cmd("A")))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);
        verify(storeRepository, never()).findByMerchantIdAndStoreId(any(), any());
        verify(storeAvailabilityRepository, never()).findActiveCovering(any(), any());
        verify(storeProductRepository, never()).findSkuQuoteRows(any(), any());
    }

    @Test
    @DisplayName("store not of this merchant → 404, windows and SKUs never read")
    void storeOfAnotherMerchant() {
        when(merchantRepository.findById(eq(merchantId))).thenReturn(Optional.of(merchant(MerchantStatus.ACTIVE)));
        when(storeRepository.findByMerchantIdAndStoreId(eq(merchantId), eq(storeId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(cmd("A")))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);
        verify(storeAvailabilityRepository, never()).findActiveCovering(any(), any());
        verify(storeProductRepository, never()).findSkuQuoteRows(any(), any());
    }

    @Test
    @DisplayName("store snapshot, merchant status (SUSPENDED reported raw), clock now, local date, open")
    void snapshot() {
        givenMerchantAndStore(MerchantStatus.SUSPENDED);

        OrderQuoteDto dto = service.get(cmd());

        assertThat(dto.storeId()).isEqualTo(storeId.getValue());
        assertThat(dto.code()).isEqualTo("S1");
        assertThat(dto.name()).isEqualTo("Store One");
        assertThat(dto.merchantStatus()).isEqualTo(MerchantStatus.SUSPENDED);
        assertThat(dto.now()).isEqualTo(NOW);
        assertThat(dto.localDate()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(dto.openStatus().open()).isTrue();
        assertThat(dto.currency()).isEqualTo("VND");
        assertThat(dto.autoAcceptOrder()).isTrue();
        assertThat(dto.maxOrdersPerDay()).isEqualTo(50);
        assertThat(dto.preparationTimeMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("a covering window found by the query closes the store")
    void coveringWindowCloses() {
        givenMerchantAndStore(MerchantStatus.ACTIVE);
        when(storeAvailabilityRepository.findActiveCovering(eq(storeId), eq(NOW))).thenReturn(List.of(
                StoreAvailability.builder().id(StoreAvailabilityId.generateId()).storeId(storeId)
                        .type(AvailabilityType.HOLIDAY).startTime(NOW.minusSeconds(60)).build()));

        OrderQuoteDto dto = service.get(cmd());

        assertThat(dto.openStatus().open()).isFalse();
        assertThat(dto.openStatus().reasons()).containsExactly(OpenReason.CLOSED_WINDOW);
        assertThat(dto.openStatus().closedWindowTypes()).containsExactly(AvailabilityType.HOLIDAY);
    }

    @Test
    @DisplayName("0 SKUs → store data only, SKU query never called")
    void noSkus() {
        givenMerchantAndStore(MerchantStatus.ACTIVE);

        assertThat(service.get(cmd()).skus()).isEmpty();
        verify(storeProductRepository, never()).findSkuQuoteRows(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("duplicates removed, request order kept, exact match; unknown/removed → found=false with null fields")
    void skus() {
        givenMerchantAndStore(MerchantStatus.ACTIVE);
        when(storeProductRepository.findSkuQuoteRows(eq(storeId), any())).thenReturn(List.of(row("B"), row("A")));

        OrderQuoteDto dto = service.get(cmd("A", "GONE", "B", "A", "a"));

        assertThat(dto.skus()).extracting(OrderQuoteDto.SkuQuote::sku).containsExactly("A", "GONE", "B", "a");
        assertThat(dto.skus()).extracting(OrderQuoteDto.SkuQuote::found).containsExactly(true, false, true, false);
        OrderQuoteDto.SkuQuote gone = dto.skus().get(1);
        assertThat(gone.productId()).isNull();
        assertThat(gone.price()).isNull();
        assertThat(gone.status()).isNull();
        assertThat(dto.skus().get(0).price()).isEqualByComparingTo("25000");

        ArgumentCaptor<Collection<SKU>> codes = ArgumentCaptor.forClass(Collection.class);
        verify(storeProductRepository).findSkuQuoteRows(eq(storeId), codes.capture());
        assertThat(codes.getValue()).containsExactly(SKU.of("A"), SKU.of("GONE"), SKU.of("B"), SKU.of("a"));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("null, blank, 101-character and NUL codes → found=false and never sent to the query; a tab is queried")
    void impossibleCodes() {
        givenMerchantAndStore(MerchantStatus.ACTIVE);
        String tooLong = "X".repeat(101);
        when(storeProductRepository.findSkuQuoteRows(eq(storeId), any())).thenReturn(List.of(row("OK"), row("T\tAB")));

        OrderQuoteDto dto = service.get(cmd(null, " ", tooLong, "A\u0000B", "OK", "T\tAB"));

        assertThat(dto.skus()).extracting(OrderQuoteDto.SkuQuote::found)
                .containsExactly(false, false, false, false, true, true);
        ArgumentCaptor<Collection<SKU>> codes = ArgumentCaptor.forClass(Collection.class);
        verify(storeProductRepository).findSkuQuoteRows(eq(storeId), codes.capture());
        assertThat(codes.getValue()).containsExactly(SKU.of("OK"), SKU.of("T\tAB"));
    }

    @Test
    @DisplayName("only impossible codes → SKU query never called")
    void onlyImpossibleCodes() {
        givenMerchantAndStore(MerchantStatus.ACTIVE);

        assertThat(service.get(cmd("", "X".repeat(101))).skus()).extracting(OrderQuoteDto.SkuQuote::found)
                .containsExactly(false, false);
        verify(storeProductRepository, never()).findSkuQuoteRows(any(), any());
    }
}
