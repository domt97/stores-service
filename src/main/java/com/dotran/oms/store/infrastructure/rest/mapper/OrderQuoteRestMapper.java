package com.dotran.oms.store.infrastructure.rest.mapper;

import com.dotran.oms.core.valueobject.Address;
import com.dotran.oms.store.application.dto.orderquote.OrderQuoteDto;
import com.dotran.oms.store.infrastructure.rest.dto.response.OrderQuoteResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderQuoteRestMapper {

    public OrderQuoteResponse toResponse(OrderQuoteDto dto) {
        var store = new OrderQuoteResponse.StoreSnapshot(dto.storeId(), dto.code(), dto.name(), dto.status(),
                toAddress(dto.address()), dto.timeZone(), dto.currency(), dto.autoAcceptOrder(), dto.allowPreOrder(),
                dto.maxOrdersPerDay(), dto.preparationTimeMinutes());
        var skus = dto.skus().stream()
                .map(s -> new OrderQuoteResponse.SkuQuote(s.sku(), s.found(), s.productId(), s.productName(),
                        s.skuName(), s.price(), s.currency(), s.status(), s.productStatus()))
                .toList();
        var open = dto.openStatus();
        return new OrderQuoteResponse(store, dto.merchantStatus(), dto.now(), dto.localDate(), open.open(),
                open.orderedReasons(), open.closedWindowTypes(), skus);
    }

    private static OrderQuoteResponse.AddressSnapshot toAddress(Address a) {
        if (a == null) {
            return null;
        }
        return new OrderQuoteResponse.AddressSnapshot(a.getPhone(), a.getAddressLine1(), a.getAddressLine2(),
                a.getWard(), a.getDistrict(), a.getProvince(), a.getCity(), a.getCountry(), a.getPostalCode());
    }
}
