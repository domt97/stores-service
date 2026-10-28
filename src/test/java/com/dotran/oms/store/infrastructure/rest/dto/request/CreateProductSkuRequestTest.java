package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateProductSkuRequest validation")
class CreateProductSkuRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private CreateProductSkuRequest.CreateProductSkuRequestBuilder valid() {
        return CreateProductSkuRequest.builder().sku("SKU-001").price(new BigDecimal("25000")).currency("VND");
    }

    private Set<String> invalidFields(CreateProductSkuRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("accepts prices 0 and 10.00, a lower-case currency with 2 decimals, and missing dimensions")
    void acceptsValidValues() {
        assertThat(invalidFields(valid().price(BigDecimal.ZERO).build())).isEmpty();
        assertThat(invalidFields(valid().price(new BigDecimal("10.00")).build())).isEmpty();
        assertThat(invalidFields(valid().weight(new BigDecimal("250.5")).build())).isEmpty();
        assertThat(invalidFields(valid().currency("usd").price(new BigDecimal("9.99")).build())).isEmpty();
    }

    @Test
    @DisplayName("rejects a negative price, a price with 3 decimals and a price above the column size")
    void rejectsInvalidPrice() {
        assertThat(invalidFields(valid().price(new BigDecimal("-1")).build())).containsExactly("price");
        assertThat(invalidFields(valid().price(new BigDecimal("10.000")).build())).containsExactly("price");
        assertThat(invalidFields(valid().price(new BigDecimal("1E17")).build())).containsExactly("price");
    }

    @Test
    @DisplayName("rejects a dimension above NUMERIC(10,2) or with more than 2 decimals")
    void rejectsInvalidDimensions() {
        assertThat(invalidFields(valid().weight(new BigDecimal("1E9")).build())).containsExactly("weight");
        assertThat(invalidFields(valid().weight(new BigDecimal("0.125")).build())).containsExactly("weight");
        assertThat(invalidFields(valid().height(new BigDecimal("100000000")).build())).containsExactly("height");
    }
}
