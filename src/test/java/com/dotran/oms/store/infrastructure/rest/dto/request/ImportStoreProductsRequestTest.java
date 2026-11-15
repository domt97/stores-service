package com.dotran.oms.store.infrastructure.rest.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImportStoreProductsRequest")
class ImportStoreProductsRequestTest {

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

    private Set<String> invalidFields(String fileName) {
        return validator.validate(ImportStoreProductsRequest.builder().fileName(fileName).build()).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("an older client's body {path, fileName} still binds with Spring's ObjectMapper; path is ignored")
    void oldBody_pathIgnored() throws Exception {
        // Spring Boot builds its ObjectMapper with Jackson2ObjectMapperBuilder (FAIL_ON_UNKNOWN_PROPERTIES off);
        // no spring.jackson.* setting or ObjectMapper bean in this service or oms-core changes that.
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

        ImportStoreProductsRequest request = mapper.readValue(
                "{\"path\":\"someone-elses-bucket\",\"fileName\":\"products.xlsx\"}", ImportStoreProductsRequest.class);

        assertThat(request.getFileName()).isEqualTo("products.xlsx");
        assertThat(invalidFields(request.getFileName())).isEmpty();
    }

    @Test
    @DisplayName("accepts a plain file name of 255 characters")
    void acceptsPlainName() {
        assertThat(invalidFields("sản phẩm.xlsx")).isEmpty();
        assertThat(invalidFields("a".repeat(255))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a/b.xlsx", "a\\b.xlsx", "line\nbreak.xlsx"})
    @DisplayName("rejects blank names, separators and control characters early (400)")
    void rejectsObviousCases(String fileName) {
        assertThat(invalidFields(fileName)).containsExactly("fileName");
    }

    @Test
    @DisplayName("rejects more than 255 characters and a missing fileName")
    void rejectsTooLongAndMissing() {
        assertThat(invalidFields("a".repeat(256))).containsExactly("fileName");
        assertThat(invalidFields(null)).containsExactly("fileName");
    }
}
