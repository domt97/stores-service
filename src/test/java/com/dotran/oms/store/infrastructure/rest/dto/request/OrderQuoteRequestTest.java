package com.dotran.oms.store.infrastructure.rest.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderQuoteRequest validation")
class OrderQuoteRequestTest {

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

    private Set<String> invalidFields(List<String> skus) {
        return validator.validate(OrderQuoteRequest.builder().skus(skus).build()).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("null list → invalid; more than 100 codes → invalid")
    void invalid() {
        assertThat(invalidFields(null)).containsExactly("skus");
        assertThat(invalidFields(Collections.nCopies(101, "A"))).containsExactly("skus");
    }

    @Test
    @DisplayName("0 codes, 100 codes, and blank/null/long codes are valid here (the service answers found=false)")
    void valid() {
        assertThat(invalidFields(List.of())).isEmpty();
        assertThat(invalidFields(Collections.nCopies(100, "A"))).isEmpty();
        assertThat(invalidFields(Arrays.asList(null, " ", "X".repeat(101)))).isEmpty();
    }
}
