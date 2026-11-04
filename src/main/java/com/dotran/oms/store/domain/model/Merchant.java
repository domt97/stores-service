package com.dotran.oms.store.domain.model;

import com.dotran.oms.core.domain.AggregateRoot;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.store.domain.enums.MerchantStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A seller company on the platform (formerly "tenant"). Owns stores. Registered by the seller itself
 * (BR-MER-06) and active immediately (BR-MER-05); its code is letters and digits only, unique on the platform,
 * case-insensitive (BR-MER-04).
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Merchant extends AggregateRoot<MerchantId> {

    /** Matches {@code merchants.code VARCHAR(50)}. */
    public static final int CODE_MAX_LENGTH = 50;

    /** BR-MER-04: ASCII letters and digits only; checked on the trimmed input, before upper-casing. */
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9]{1," + CODE_MAX_LENGTH + "}");

    private String code;
    private String name;
    private MerchantStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static Merchant register(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Merchant code is required");
        }
        if (!CODE_PATTERN.matcher(code.trim()).matches()) {
            throw new ValidationException("Merchant code must be 1-" + CODE_MAX_LENGTH + " letters (A-Z) or digits");
        }
        String normalizedCode = normalizeCode(code);
        if (name == null || name.isBlank()) {
            throw new ValidationException("Merchant name is required");
        }
        Instant now = Instant.now();
        return Merchant.builder()
                .id(MerchantId.newMerchantId())
                .code(normalizedCode)
                .name(name.trim())
                .status(MerchantStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** Codes are compared case-insensitively: stored trimmed and upper-case. */
    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }
}
