package com.dotran.oms.store.application.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductImportLimits")
class ProductImportLimitsTest {

    private static final long MB = 1024L * 1024;
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("slots from the heap when permits is 0: one per 128 MB, one share kept free, at least 1, at most maxPermits")
    void effectivePermits_fromHeap() {
        ProductImportLimits limits = new ProductImportLimits();

        assertThat(limits.effectivePermits(512 * MB)).isEqualTo(3);
        assertThat(limits.effectivePermits(100 * MB)).isEqualTo(1);
        assertThat(limits.effectivePermits(2048 * MB)).isEqualTo(4); // 15 by heap, capped by the connection pool share
        limits.setMaxPermits(8);
        assertThat(limits.effectivePermits(2048 * MB)).isEqualTo(8);
    }

    @Test
    @DisplayName("an explicit permits value wins")
    void effectivePermits_explicit() {
        ProductImportLimits limits = new ProductImportLimits();
        limits.setPermits(5);

        assertThat(limits.effectivePermits(512 * MB)).isEqualTo(5);
    }

    @Test
    @DisplayName("defaults are valid; zero or negative sizes, negative permits and a zero heap share are rejected at startup")
    void validation() {
        assertThat(validator.validate(new ProductImportLimits())).isEmpty();

        ProductImportLimits bad = new ProductImportLimits();
        bad.setPermits(-1);
        bad.setHeapPerImportBytes(0);
        bad.setMaxBytes(0);
        bad.setMaxPermits(0);
        bad.setMinInflateRatio(0);

        assertThat(validator.validate(bad)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("permits", "heapPerImportBytes", "maxBytes", "maxPermits", "minInflateRatio");
    }
}
