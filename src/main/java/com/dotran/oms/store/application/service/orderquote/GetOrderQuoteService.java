package com.dotran.oms.store.application.service.orderquote;

import com.dotran.oms.core.annotation.UseCase;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.orderquote.GetOrderQuoteCmd;
import com.dotran.oms.store.application.dto.orderquote.OrderQuoteDto;
import com.dotran.oms.store.application.dto.orderquote.SkuQuoteRow;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.usecase.orderquote.GetOrderQuoteUseCase;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.domain.enums.OpenReason;
import com.dotran.oms.store.domain.model.Merchant;
import com.dotran.oms.store.domain.model.Store;
import com.dotran.oms.store.domain.model.StoreAvailability;
import com.dotran.oms.store.domain.valueobject.OpenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Internal order quote for order-service (ADR 0007): merchant (404) → the merchant's store (404) → the windows covering
 * now → the requested SKUs → open status. Read-only and advisory: statuses and currencies are returned raw, and
 * order-service applies the placement rules. Nothing is written.
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
public class GetOrderQuoteService implements GetOrderQuoteUseCase {

    /** {@code product_skus.sku} is VARCHAR(100): a longer code cannot exist. */
    static final int MAX_SKU_LENGTH = 100;

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreAvailabilityRepository storeAvailabilityRepository;
    private final StoreProductRepository storeProductRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public OrderQuoteDto get(GetOrderQuoteCmd cmd) {
        Merchant merchant = merchantRepository.findById(cmd.merchantId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_MERCHANT_NOT_FOUND));
        Store store = storeRepository.findByMerchantIdAndStoreId(cmd.merchantId(), cmd.storeId())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_STORE_NOT_FOUND));
        Instant now = clock.instant();

        List<StoreAvailability> windows = storeAvailabilityRepository.findActiveCovering(store.getId(), now);
        OpenStatus openStatus = store.openAt(now, windows);
        if (openStatus.reasons().contains(OpenReason.INVALID_SCHEDULE)) {
            log.warn("Store schedule cannot be read (time zone or business hours): storeId={}", store.getId().getValue());
        }

        // Distinct codes in request order, matched exactly. A null, blank, too long or NUL-containing code cannot exist
        // (Postgres text refuses NUL; other control characters can be stored by the SKU write paths): not queried.
        Set<String> requested = new LinkedHashSet<>(cmd.skus() == null ? List.of() : cmd.skus());
        List<SKU> lookup = requested.stream().filter(GetOrderQuoteService::canExist).map(SKU::of).toList();
        Map<String, SkuQuoteRow> rows = lookup.isEmpty() ? Map.of()
                : storeProductRepository.findSkuQuoteRows(store.getId(), lookup).stream()
                        .collect(Collectors.toMap(SkuQuoteRow::sku, Function.identity(), (a, b) -> a));
        List<OrderQuoteDto.SkuQuote> skus = requested.stream()
                .map(code -> rows.containsKey(code) ? OrderQuoteDto.SkuQuote.of(rows.get(code))
                        : OrderQuoteDto.SkuQuote.notFound(code))
                .toList();

        var config = store.getConfig();
        return OrderQuoteDto.builder()
                .storeId(store.getId().getValue())
                .code(store.getCode())
                .name(store.getName())
                .status(store.getStatus())
                .address(store.getAddress())
                .timeZone(config == null ? null : config.getTimeZone())
                .currency(config == null ? null : config.getCurrency())
                .autoAcceptOrder(config != null && config.isAutoAcceptOrder())
                .allowPreOrder(config != null && config.isAllowPreOrder())
                .maxOrdersPerDay(config == null ? null : config.getMaxOrdersPerDay())
                .preparationTimeMinutes(config == null ? null : config.getPreparationTimeMinutes())
                .merchantStatus(merchant.getStatus())
                .now(now)
                .localDate(store.localDate(now).orElse(null))
                .openStatus(openStatus)
                .skus(skus)
                .build();
    }

    private static boolean canExist(String code) {
        return code != null && !code.isBlank() && code.length() <= MAX_SKU_LENGTH
                && code.indexOf('\u0000') < 0;
    }
}
