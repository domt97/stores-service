package com.dotran.oms.store.infrastructure.rest.internal;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.valueobject.Address;
import com.dotran.oms.store.application.command.orderquote.GetOrderQuoteCmd;
import com.dotran.oms.store.application.dto.orderquote.OrderQuoteDto;
import com.dotran.oms.store.application.usecase.orderquote.GetOrderQuoteUseCase;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import com.dotran.oms.store.domain.enums.OpenReason;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.valueobject.OpenStatus;
import com.dotran.oms.store.infrastructure.rest.dto.request.OrderQuoteRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.OrderQuoteResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.OrderQuoteRestMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderQuoteInternalController")
class OrderQuoteInternalControllerTest {

    @Mock private GetOrderQuoteUseCase getOrderQuoteUseCase;

    private OrderQuoteDto dto(Address address) {
        return OrderQuoteDto.builder().storeId(UUID.randomUUID()).code("S1").name("Store One").status(StoreStatus.ACTIVE)
                .address(address).timeZone("Asia/Ho_Chi_Minh").currency("VND").merchantStatus(MerchantStatus.ACTIVE)
                .now(Instant.parse("2026-11-16T03:00:00Z")).localDate(LocalDate.of(2026, 11, 16))
                .openStatus(new OpenStatus(false, EnumSet.of(OpenReason.CLOSED_WINDOW, OpenReason.STORE_NOT_ACTIVE),
                        List.of(AvailabilityType.HOLIDAY)))
                .skus(List.of(OrderQuoteDto.SkuQuote.notFound("X")))
                .build();
    }

    @Test
    @DisplayName("cmd carries merchant and store from the path and the SKU list; response is mapped")
    void delegatesAndMaps() {
        var controller = new OrderQuoteInternalController(getOrderQuoteUseCase, new OrderQuoteRestMapper());
        UUID merchantId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(getOrderQuoteUseCase.get(any())).thenReturn(dto(Address.builder().city("HCM").build()));

        OrderQuoteResponse response = controller.orderQuote(merchantId, storeId,
                OrderQuoteRequest.builder().skus(List.of("A", "B")).build());

        ArgumentCaptor<GetOrderQuoteCmd> cmd = ArgumentCaptor.forClass(GetOrderQuoteCmd.class);
        verify(getOrderQuoteUseCase).get(cmd.capture());
        assertThat(cmd.getValue().merchantId()).isEqualTo(MerchantId.of(merchantId));
        assertThat(cmd.getValue().storeId()).isEqualTo(StoreId.of(storeId));
        assertThat(cmd.getValue().skus()).containsExactly("A", "B");

        assertThat(response.open()).isFalse();
        assertThat(response.reasons()).containsExactly(OpenReason.STORE_NOT_ACTIVE, OpenReason.CLOSED_WINDOW);
        assertThat(response.closedWindowTypes()).containsExactly(AvailabilityType.HOLIDAY);
        assertThat(response.store().address().city()).isEqualTo("HCM");
        assertThat(response.store().name()).isEqualTo("Store One");
        assertThat(response.skus()).singleElement().satisfies(s -> {
            assertThat(s.found()).isFalse();
            assertThat(s.productId()).isNull();
        });
    }

    @Test
    @DisplayName("a store without an address → address null")
    void noAddress() {
        assertThat(new OrderQuoteRestMapper().toResponse(dto(null)).store().address()).isNull();
    }

    @Test
    @DisplayName("internal: @Hidden (not in public OpenAPI) and under /internal/v1/store")
    void hiddenAndInternal() {
        assertThat(OrderQuoteInternalController.class.isAnnotationPresent(Hidden.class)).isTrue();
        assertThat(OrderQuoteInternalController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/internal/v1/store");
    }
}
