package com.dotran.oms.store.infrastructure.rest.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CreateImportUploadRequest")
class CreateImportUploadRequestTest {

    /** Spring Boot builds its ObjectMapper with this builder (ACCEPT_FLOAT_AS_INT on, unknown properties ignored). */
    private static final ObjectMapper MAPPER = Jackson2ObjectMapperBuilder.json().build();
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

    private static Set<String> invalidFields(String json) throws Exception {
        CreateImportUploadRequest request = MAPPER.readValue(json, CreateImportUploadRequest.class);
        return validator.validate(request).stream().map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a whole size binds exactly; a numeric string binds too (same value)")
    void wholeSize() throws Exception {
        assertThat(invalidFields("{\"sizeBytes\":5242880}")).isEmpty();
        assertThat(invalidFields("{\"sizeBytes\":1}")).isEmpty();
        assertThat(MAPPER.readValue("{\"sizeBytes\":\"5\"}", CreateImportUploadRequest.class).getSizeBytes())
                .isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(MAPPER.readValue("{\"sizeBytes\":5242880}", CreateImportUploadRequest.class).getSizeBytes()
                .longValueExact()).isEqualTo(5242880L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"sizeBytes\":1.5}", "{\"sizeBytes\":1.0}", "{\"sizeBytes\":0}", "{\"sizeBytes\":-3}",
            "{\"sizeBytes\":9999999999999999999}", "{}", "{\"sizeBytes\":null}"})
    @DisplayName("a fraction (not rounded to a wrong length), 0, negative, more than 18 digits or missing → 400")
    void rejected(String json) throws Exception {
        assertThat(invalidFields(json)).containsExactly("sizeBytes");
    }

    @Test
    @DisplayName("a non-numeric size cannot be read → 400 (HttpMessageNotReadable)")
    void notANumber() {
        assertThatThrownBy(() -> MAPPER.readValue("{\"sizeBytes\":\"abc\"}", CreateImportUploadRequest.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
