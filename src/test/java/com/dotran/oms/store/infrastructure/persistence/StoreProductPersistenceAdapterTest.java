package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.valueobject.Money;
import com.dotran.oms.core.valueobject.PriceRange;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.dotran.oms.store.infrastructure.mapper.StoreProductPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreProductPersistenceAdapter")
class StoreProductPersistenceAdapterTest {

    @Mock private SpringDataStoreProductRepository repository;
    @Mock private StoreProductPersistenceMapper mapper;

    @InjectMocks
    private StoreProductPersistenceAdapter storeProductPersistenceAdapter;

    private StoreProduct buildProduct() {
        StoreProduct p = StoreProduct.builder()
                .id(ProductId.newProductId())
                .storeId(StoreId.newStoreId())
                .name("Product")
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();
        p.initState();
        return p;
    }

    @Test
    @DisplayName("create: maps, persists, and returns domain object")
    void create_savesAndReturnsDomain() {
        StoreProduct product = buildProduct();
        StoreProductEntity entity = mock(StoreProductEntity.class);
        StoreProductEntity savedEntity = mock(StoreProductEntity.class);

        when(mapper.fromStoreProduct(product)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(mapper.fromEntity(savedEntity)).thenReturn(product);

        StoreProduct result = storeProductPersistenceAdapter.create(product);

        assertThat(result).isEqualTo(product);
    }

    @Test
    @DisplayName("getByStoreIdAndProductId: returns mapped domain when found")
    void getByIds_returnsDomain_whenFound() {
        StoreId storeId = StoreId.newStoreId();
        ProductId productId = ProductId.newProductId();
        StoreProductEntity entity = mock(StoreProductEntity.class);
        StoreProduct product = buildProduct();

        when(repository.findByIdAndStoreId(productId.getValue(), storeId.getValue()))
                .thenReturn(Optional.of(entity));
        when(mapper.fromEntity(entity)).thenReturn(product);

        Optional<StoreProduct> result = storeProductPersistenceAdapter.getByStoreIdAndProductId(storeId, productId);

        assertThat(result).contains(product);
    }

    @Test
    @DisplayName("findExistingProductIds: returns empty set without querying when input is null")
    void findExistingProductIds_returnsEmpty_whenNull() {
        Set<ProductId> result = storeProductPersistenceAdapter.findExistingProductIds(StoreId.newStoreId(), null);

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findExistingProductIds: returns empty set without querying when input is empty")
    void findExistingProductIds_returnsEmpty_whenEmpty() {
        Set<ProductId> result = storeProductPersistenceAdapter.findExistingProductIds(StoreId.newStoreId(), List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findExistingProductIds: queries scoped by store and returns only the ids found there")
    void findExistingProductIds_queriesByStore_andMapsIds() {
        StoreId storeId = StoreId.newStoreId();
        ProductId inStore = ProductId.newProductId();
        ProductId elsewhere = ProductId.newProductId();

        when(repository.findIdsByStoreIdAndIdIn(storeId.getValue(), List.of(inStore.getValue(), elsewhere.getValue())))
                .thenReturn(Set.of(inStore.getValue()));

        Set<ProductId> result = storeProductPersistenceAdapter.findExistingProductIds(storeId, List.of(inStore, elsewhere));

        assertThat(result).containsExactly(inStore);
    }

    @Test
    @DisplayName("saveAll: throws ValidationException when list is null")
    void saveAll_throwsValidationException_whenListIsNull() {
        assertThatThrownBy(() -> storeProductPersistenceAdapter.saveAll(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("saveAll: throws ValidationException when list is empty")
    void saveAll_throwsValidationException_whenListIsEmpty() {
        assertThatThrownBy(() -> storeProductPersistenceAdapter.saveAll(List.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("getByStoreIdAndProductId: returns empty when not found")
    void getByIds_returnsEmpty_whenNotFound() {
        StoreId storeId = StoreId.newStoreId();
        ProductId productId = ProductId.newProductId();

        when(repository.findByIdAndStoreId(productId.getValue(), storeId.getValue()))
                .thenReturn(Optional.empty());

        Optional<StoreProduct> result = storeProductPersistenceAdapter.getByStoreIdAndProductId(storeId, productId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchProducts: delegates to repository and maps paged result")
    void searchProducts_returnsMappedPagedResult() {
        StoreId storeId = StoreId.newStoreId();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();
        StoreProduct product = buildProduct();
        StoreProductEntity entity = mock(StoreProductEntity.class);

        Page<StoreProductEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(mapper.fromEntity(entity)).thenReturn(product);

        PagedResult<StoreProduct> result = storeProductPersistenceAdapter.searchProducts(storeId, null, pageRequest);

        assertThat(result.getContent()).containsExactly(product);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchProducts: specification applies price range predicates when both bounds are set")
    @SuppressWarnings("unchecked")
    void searchProducts_withFullPriceRange_appliesPredicates() {
        StoreId storeId = StoreId.newStoreId();
        BigDecimal minPrice = new BigDecimal("10.00");
        BigDecimal maxPrice = new BigDecimal("100.00");
        PriceRange priceRange = PriceRange.builder()
                .from(Money.builder().amount(minPrice).currency("USD").build())
                .to(Money.builder().amount(maxPrice).currency("USD").build())
                .build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        ArgumentCaptor<Specification<StoreProductEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        when(repository.findAll(specCaptor.capture(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Page.empty());

        storeProductPersistenceAdapter.searchProducts(storeId, priceRange, pageRequest);

        Root<StoreProductEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

        specCaptor.getValue().toPredicate(root, query, cb);

        verify(root).join("skus");
        verify(cb).greaterThanOrEqualTo(any(), any(Comparable.class));
        verify(cb).lessThanOrEqualTo(any(), any(Comparable.class));
        verify(query).distinct(true);
    }

    @Test
    @DisplayName("searchProducts: specification skips price predicates when bounds are null")
    @SuppressWarnings("unchecked")
    void searchProducts_withNullPriceBounds_skipsRangePredicates() {
        StoreId storeId = StoreId.newStoreId();
        PriceRange priceRange = PriceRange.builder().from(null).to(null).build();
        DomainPageRequest pageRequest = DomainPageRequest.builder().pageNumber(0).pageSize(10).build();

        ArgumentCaptor<Specification<StoreProductEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        when(repository.findAll(specCaptor.capture(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(Page.empty());

        storeProductPersistenceAdapter.searchProducts(storeId, priceRange, pageRequest);

        Root<StoreProductEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

        specCaptor.getValue().toPredicate(root, query, cb);

        verify(root).join("skus");
        verify(query).distinct(true);
    }

    @Test
    @DisplayName("saveAll: saves all entities and returns domain objects")
    void saveAll_savesAndReturnsDomains() {
        StoreProduct product = buildProduct();
        StoreProductEntity entity = mock(StoreProductEntity.class);
        StoreProductEntity savedEntity = mock(StoreProductEntity.class);

        when(mapper.fromStoreProduct(product)).thenReturn(entity);
        when(repository.saveAllAndFlush(any())).thenReturn(List.of(savedEntity));
        when(mapper.fromEntity(savedEntity)).thenReturn(product);

        List<StoreProduct> result = storeProductPersistenceAdapter.saveAll(List.of(product));

        assertThat(result).containsExactly(product);
    }
}
