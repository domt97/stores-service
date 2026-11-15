package com.dotran.oms.store.infrastructure.rest.internal;

import com.dotran.oms.core.annotation.WebAdapter;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.store.application.command.orderquote.GetOrderQuoteCmd;
import com.dotran.oms.store.application.usecase.orderquote.GetOrderQuoteUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.OrderQuoteRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.OrderQuoteResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.OrderQuoteRestMapper;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal endpoints for other services (ADR 0007): not in the public OpenAPI, not proxied by oms-web. No
 * authentication exists yet: reachable only inside the network. {@code POST} because of the SKU list; read-only.
 * Answers: 200; 400 (no list, more than 100 codes, a path id that is not a UUID); 404 merchant, or store not of this
 * merchant (final for the caller).
 */
@Hidden
@WebAdapter
@RestController
@RequestMapping("/internal/v1/store")
@RequiredArgsConstructor
public class OrderQuoteInternalController {

    private final GetOrderQuoteUseCase getOrderQuoteUseCase;
    private final OrderQuoteRestMapper orderQuoteRestMapper;

    @PostMapping("/merchants/{merchantId}/stores/{storeId}/order-quote")
    public OrderQuoteResponse orderQuote(@PathVariable UUID merchantId,
                                         @PathVariable UUID storeId,
                                         @RequestBody @Valid OrderQuoteRequest request) {
        return orderQuoteRestMapper.toResponse(getOrderQuoteUseCase.get(GetOrderQuoteCmd.builder()
                .merchantId(MerchantId.of(merchantId))
                .storeId(StoreId.of(storeId))
                .skus(request.getSkus())
                .build()));
    }
}
