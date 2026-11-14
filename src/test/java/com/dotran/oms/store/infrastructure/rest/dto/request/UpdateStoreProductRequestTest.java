package com.dotran.oms.store.infrastructure.rest.dto.request;

import com.dotran.oms.store.domain.enums.ProductSkuStatus;
import com.dotran.oms.store.domain.enums.ProductStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Controller tests call methods directly, so request constraints are checked here with a plain Validator. */
@DisplayName("Update-product request validation")
class UpdateStoreProductRequestTest {

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

    private static UpdateProductSkuRequest sku(String code) {
        return UpdateProductSkuRequest.builder().sku(code).price(new BigDecimal("25000")).currency("VND")
                .status(ProductSkuStatus.ACTIVE).build();
    }

    private UpdateStoreProductRequest.UpdateStoreProductRequestBuilder valid() {
        return UpdateStoreProductRequest.builder().version(0L).name("Milk Tea").status(ProductStatus.ACTIVE)
                .skus(List.of(sku("A"))).images(List.of());
    }

    private Set<String> invalid(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .map(path -> path.replaceAll("\\[\\d+]", "[]"))
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a minimal request is valid; optional fields may be omitted (they are cleared)")
    void minimalRequest_valid() {
        assertThat(invalid(valid().build())).isEmpty();
    }

    @Test
    @DisplayName("version, name, status, skus and images are required")
    void requiredFields() {
        assertThat(invalid(valid().version(null).build())).containsExactly("version");
        assertThat(invalid(valid().version(-1L).build())).containsExactly("version");
        assertThat(invalid(valid().name(" ").build())).containsExactly("name");
        assertThat(invalid(valid().status(null).build())).containsExactly("status");
        assertThat(invalid(valid().skus(List.of()).build())).containsExactly("skus");
        assertThat(invalid(valid().images(null).build())).containsExactly("images");
    }

    @Test
    @DisplayName("limits: 100 SKUs, 20 images, 5000-character description, 255-character name and thumbnail")
    void limits() {
        List<UpdateProductSkuRequest> skus101 = IntStream.range(0, 101).mapToObj(i -> sku("S" + i)).toList();
        List<UpdateProductImageRequest> images21 = IntStream.range(0, 21)
                .mapToObj(i -> UpdateProductImageRequest.builder().imageUrl("https://img/" + i).displayOrder(i).build()).toList();

        assertThat(invalid(valid().skus(skus101).build())).containsExactly("skus");
        assertThat(invalid(valid().images(images21).build())).containsExactly("images");
        assertThat(invalid(valid().description("x".repeat(5001)).build())).containsExactly("description");
        assertThat(invalid(valid().name("x".repeat(256)).build())).containsExactly("name");
        assertThat(invalid(valid().thumbnailUrl("x".repeat(256)).build())).containsExactly("thumbnailUrl");
        assertThat(invalid(valid().skus(IntStream.range(0, 100).mapToObj(i -> sku("S" + i)).toList()).build())).isEmpty();
    }

    @Test
    @DisplayName("null list elements are rejected (no NullPointerException later)")
    void nullElements() {
        assertThat(invalid(valid().skus(Arrays.asList(sku("A"), null)).build())).anyMatch(path -> path.startsWith("skus[]"));
        assertThat(invalid(valid().images(Collections.singletonList(null)).build())).anyMatch(path -> path.startsWith("images[]"));
    }

    @Test
    @DisplayName("SKU fields: code required ≤ 100, price ≥ 0 with ≤ 2 decimals, 3-letter currency, status required")
    void skuFields() {
        assertThat(invalid(sku(" "))).containsExactly("sku");
        assertThat(invalid(sku("X".repeat(101)))).containsExactly("sku");
        assertThat(invalid(UpdateProductSkuRequest.builder().sku("A").price(new BigDecimal("-1")).currency("VND").status(ProductSkuStatus.ACTIVE).build()))
                .containsExactly("price");
        assertThat(invalid(UpdateProductSkuRequest.builder().sku("A").price(new BigDecimal("1.005")).currency("VND").status(ProductSkuStatus.ACTIVE).build()))
                .containsExactly("price");
        assertThat(invalid(UpdateProductSkuRequest.builder().sku("A").price(BigDecimal.ONE).currency("VNDX").status(ProductSkuStatus.ACTIVE).build()))
                .containsExactly("currency");
        assertThat(invalid(UpdateProductSkuRequest.builder().sku("A").price(BigDecimal.ONE).currency("VND").build()))
                .containsExactly("status");
        assertThat(invalid(UpdateProductSkuRequest.builder().sku("A").price(BigDecimal.ONE).currency("VND").status(ProductSkuStatus.ACTIVE)
                .weight(new BigDecimal("1E9")).build())).containsExactly("weight");
    }

    @Test
    @DisplayName("image: URL required ≤ 2048, display order required ≥ 0")
    void imageFields() {
        assertThat(invalid(UpdateProductImageRequest.builder().imageUrl("").displayOrder(0).build())).containsExactly("imageUrl");
        assertThat(invalid(UpdateProductImageRequest.builder().imageUrl("x".repeat(2049)).displayOrder(0).build())).containsExactly("imageUrl");
        assertThat(invalid(UpdateProductImageRequest.builder().imageUrl("https://img").displayOrder(-1).build())).containsExactly("displayOrder");
    }

    @Test
    @DisplayName("create-product now has the same limits: 100 SKUs, 20 images, 5000-character description, no null elements")
    void createLimits() {
        CreateProductSkuRequest createSku = CreateProductSkuRequest.builder().sku("A").price(BigDecimal.ONE).currency("VND").build();
        CreateStoreProductRequest.CreateStoreProductRequestBuilder create = CreateStoreProductRequest.builder().name("P").skus(List.of(createSku));

        assertThat(invalid(create.build())).isEmpty();
        assertThat(invalid(create.description("x".repeat(5001)).build())).containsExactly("description");
        List<CreateProductSkuRequest> skus101 = new ArrayList<>(Collections.nCopies(101, createSku));
        assertThat(invalid(CreateStoreProductRequest.builder().name("P").skus(skus101).build())).containsExactly("skus");
        List<CreateProductImageRequest> images21 = Collections.nCopies(21,
                CreateProductImageRequest.builder().imageUrl("https://img").displayOrder(1).build());
        assertThat(invalid(CreateStoreProductRequest.builder().name("P").skus(List.of(createSku)).images(images21).build())).containsExactly("images");
        assertThat(invalid(CreateStoreProductRequest.builder().name("P").skus(Arrays.asList(createSku, null)).build())).anyMatch(path -> path.startsWith("skus[]"));
    }
}
