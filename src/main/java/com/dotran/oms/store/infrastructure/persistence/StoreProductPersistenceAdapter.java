package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.domain.exception.ProductVersionConflictException;
import com.dotran.oms.store.domain.exception.SkuCodeInUseException;
import com.dotran.oms.store.domain.model.ProductChangeSet;
import com.dotran.oms.store.domain.model.ProductSku;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductSkuEntity;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import com.dotran.oms.core.exception.ValidationException;
import com.dotran.oms.core.util.CollectionUtils;
import com.dotran.oms.core.util.StringUtils;
import com.dotran.oms.core.valueobject.PriceRange;
import com.dotran.oms.store.application.repository.StoreProductRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.StoreCollectionId;
import com.dotran.oms.store.domain.model.StoreProduct;
import com.dotran.oms.store.infrastructure.mapper.StoreProductPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.ProductCollectionEntity;
import com.dotran.oms.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StoreProductPersistenceAdapter implements StoreProductRepository {

    static final String SKU_CODE_CONSTRAINT = "uk_product_skus_store_sku";
    private static final String ID_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "name", ID_FIELD);

    private final SpringDataStoreProductRepository repository;
    private final StoreProductPersistenceMapper mapper;

    @Override
    public StoreProduct create(StoreProduct storeProduct) {
        StoreProductEntity storeProductEntity = toNewEntity(storeProduct);

        try {
            StoreProductEntity savedEntity = repository.saveAndFlush(storeProductEntity);
            return mapper.fromEntity(savedEntity);
        } catch (DataIntegrityViolationException ex) {
            throw translateSkuCodeConflict(ex);
        } catch (CannotAcquireLockException ex) {
            throw new SkuCodeInUseException(Constants.ERROR_MSG_SKU_CODE_CONCURRENT);
        }
    }

    @Override
    public Optional<StoreProduct> getByStoreIdAndProductId(StoreId storeId, ProductId productId) {
        return repository.findByIdAndStoreId(productId.getValue(), storeId.getValue())
                .map(mapper::fromEntity);
    }

    @Override
    public Set<ProductId> findExistingProductIds(StoreId storeId, Collection<ProductId> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return new HashSet<>();
        }

        return repository.findIdsByStoreIdAndIdIn(storeId.getValue(), productIds.stream().map(ProductId::getValue).toList())
                .stream()
                .map(ProductId::of)
                .collect(Collectors.toSet());
    }

    @Override
    public PagedResult<StoreProduct> searchProducts(StoreId storeId,
                                                    PriceRange priceRange,
                                                    StoreCollectionId excludeCollectionId,
                                                    DomainPageRequest pageRequest) {
        Specification<StoreProductEntity> specification = buildSearchSpecification(storeId, priceRange, excludeCollectionId);

        Page<StoreProduct> storeProductPage = repository.findAll(specification, toSearchPageRequest(pageRequest))
                .map(mapper::fromEntity);

        return PagedResult.of(storeProductPage);
    }

    @Override
    public PagedResult<StoreProduct> findInCollection(StoreId storeId, StoreCollectionId collectionId, DomainPageRequest pageRequest) {
        // Order is fixed by the query (name, id); the page request carries only number and size.
        Page<StoreProduct> page = repository
                .findInCollection(storeId.getValue(), collectionId.getValue(),
                        PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize()))
                .map(mapper::fromEntity);

        return PagedResult.of(page);
    }

    /**
     * Search order: the requested field (default {@code createdAt DESC}) with {@code id} as the last key, so offset
     * paging never repeats or skips rows that share a value. Only known fields are accepted: an unknown field or
     * direction is a 400, not a query-time 500.
     */
    static PageRequest toSearchPageRequest(DomainPageRequest pageRequest) {
        boolean defaultSort = StringUtils.isNullOrEmpty(pageRequest.getSortBy());
        String sortBy = defaultSort ? "createdAt" : pageRequest.getSortBy();
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new ValidationException(Constants.ERROR_MSG_INVALID_SORT_FIELD);
        }

        Sort.Direction direction;
        if (StringUtils.isNullOrEmpty(pageRequest.getDirection())) {
            direction = defaultSort ? Sort.Direction.DESC : Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.fromOptionalString(pageRequest.getDirection())
                    .orElseThrow(() -> new ValidationException(Constants.ERROR_MSG_INVALID_SORT_DIRECTION));
        }

        Sort sort = Sort.by(direction, sortBy);
        if (!ID_FIELD.equals(sortBy)) {
            sort = sort.and(Sort.by(direction, ID_FIELD));
        }

        return PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), sort);
    }

    @Override
    public List<StoreProduct> saveAll(List<StoreProduct> storeProductList) {
        if (CollectionUtils.isEmpty(storeProductList)) {
            log.error("Store product list is empty, nothing to save.");
            throw new ValidationException("Store product list is empty, nothing to save.");
        }

        List<StoreProductEntity> storeProductEntityList = new ArrayList<>();
        for (StoreProduct storeProduct : storeProductList) {
            storeProductEntityList.add(toNewEntity(storeProduct));
        }
        try {
            return repository.saveAllAndFlush(storeProductEntityList)
                    .stream()
                    .map(mapper::fromEntity)
                    .toList();
        } catch (DataIntegrityViolationException ex) {
            throw translateSkuCodeConflict(ex);
        } catch (CannotAcquireLockException ex) {
            throw new SkuCodeInUseException(Constants.ERROR_MSG_SKU_CODE_CONCURRENT);
        }
    }

    /** New product with its SKUs in code order: concurrent inserts of overlapping codes take index locks in the same order. */
    private StoreProductEntity toNewEntity(StoreProduct storeProduct) {
        StoreProductEntity entity = mapper.fromStoreProduct(storeProduct);
        List<ProductSkuEntity> skus = new ArrayList<>(mapper.fromProductSkus(storeProduct.getSkus(), entity));
        skus.sort(Comparator.comparing(ProductSkuEntity::getSku));
        entity.setSkus(skus);
        entity.setImages(mapper.fromProductImages(storeProduct.getImages(), entity));
        return entity;
    }

    @Override
    public boolean bumpVersion(StoreId storeId, ProductId productId, long expectedVersion, Instant updatedAt) {
        return repository.bumpVersion(productId.getValue(), storeId.getValue(), expectedVersion, updatedAt) > 0;
    }

    @Override
    public long currentVersion(StoreId storeId, ProductId productId) {
        return repository.findVersion(productId.getValue(), storeId.getValue())
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_PRODUCT_NOT_FOUND));
    }

    @Override
    public Set<SKU> findSkuCodesInStore(StoreId storeId, Collection<SKU> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return new HashSet<>();
        }
        return repository.findSkuCodesInStore(storeId.getValue(), codes.stream().map(SKU::getValue).toList())
                .stream().map(SKU::of).collect(Collectors.toSet());
    }

    @Override
    public Set<SKU> findSkuCodesOfOtherProducts(StoreId storeId, ProductId productId, Collection<SKU> codes) {
        if (CollectionUtils.isEmpty(codes)) {
            return new HashSet<>();
        }
        return repository.findSkuCodesOfOtherProducts(storeId.getValue(), productId.getValue(),
                        codes.stream().map(SKU::getValue).toList())
                .stream().map(SKU::of).collect(Collectors.toSet());
    }

    /**
     * Applies the change set to the managed product entity in place (Hibernate owns the child collections; replacing
     * them would break orphan removal), then flushes so new SKU ids exist and a code collision surfaces here as a 409.
     */
    @Override
    public StoreProduct applyChanges(StoreProduct product, ProductChangeSet changes, long newVersion) {
        StoreProductEntity entity = repository.findById(product.getId().getValue())
                .filter(found -> found.getStoreId().equals(product.getStoreId().getValue()))
                .orElseThrow(() -> new NotFoundException(Constants.ERROR_MSG_PRODUCT_NOT_FOUND));

        if (changes.productFieldsChanged()) {
            entity.setName(product.getName());
            entity.setDescription(product.getDescription());
            entity.setThumbnailUrl(product.getThumbnailUrl());
            entity.setCategoryId(product.getCategoryId() != null ? product.getCategoryId().getValue() : null);
            entity.setStatus(product.getStatus());
        }
        if (changes.imagesChanged()) {
            entity.getImages().clear();
            entity.getImages().addAll(mapper.fromProductImages(product.getImages(), entity));
        }

        Map<UUID, ProductSkuEntity> skuById = entity.getSkus().stream()
                .collect(Collectors.toMap(ProductSkuEntity::getId, Function.identity()));
        for (ProductSku sku : changes.updated()) {
            copySkuValues(sku, skuById.get(sku.getId().getValue()));
        }
        for (ProductSku sku : changes.removed()) {
            skuById.get(sku.getId().getValue()).setRemovedAt(sku.getRemovedAt());
        }
        // Sorted by code: concurrent inserts of overlapping codes take index locks in the same order.
        changes.added().stream()
                .sorted(Comparator.comparing(sku -> sku.getSku().getValue()))
                .forEach(sku -> entity.getSkus().add(mapper.fromProductSku(sku, entity)));

        // The column is updatable=false: this only keeps the in-memory value equal to what bumpVersion wrote.
        entity.setVersion(newVersion);
        entity.setUpdatedAt(product.getUpdatedAt());

        try {
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateSkuCodeConflict(ex);
        } catch (CannotAcquireLockException ex) {
            throw new ProductVersionConflictException(Constants.ERROR_MSG_PRODUCT_CONCURRENT_UPDATE);
        }
        return mapper.fromEntity(entity);
    }

    private static void copySkuValues(ProductSku sku, ProductSkuEntity target) {
        target.setName(sku.getName());
        target.setPrice(sku.getPrice());
        target.setCurrency(sku.getCurrency());
        target.setWeight(sku.getWeight());
        target.setLength(sku.getLength());
        target.setWidth(sku.getWidth());
        target.setHeight(sku.getHeight());
        target.setStatus(sku.getStatus());
        target.setRemovedAt(sku.getRemovedAt());
    }

    /** uk_product_skus_store_sku → 409; any other integrity error is rethrown unchanged. */
    static RuntimeException translateSkuCodeConflict(DataIntegrityViolationException ex) {
        if (violates(ex, SKU_CODE_CONSTRAINT)) {
            return new SkuCodeInUseException(String.format(Constants.ERROR_MSG_SKU_CODE_IN_USE, "(database)"));
        }
        return ex;
    }

    /**
     * Hibernate reports the constraint name ({@code ConstraintViolationException}); with JDBC batching the driver
     * message sits further down the chain (BatchUpdateException → PSQLException), so every cause is checked.
     */
    private static boolean violates(Throwable ex, String constraint) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && constraint.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            if (cause.getMessage() != null && cause.getMessage().contains(constraint)) {
                return true;
            }
            if (cause instanceof java.sql.SQLException sql && sql.getNextException() != null
                    && sql.getNextException().getMessage() != null
                    && sql.getNextException().getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds JPA Specification for product search with dynamic criteria.
     *
     * @param storeId    the store ID to filter products
     * @param priceRange optional price range filter
     * @param excludeCollectionId optional collection whose products are left out (NOT EXISTS)
     * @return JPA Specification
     */
    private Specification<StoreProductEntity> buildSearchSpecification(StoreId storeId, PriceRange priceRange,
                                                                       StoreCollectionId excludeCollectionId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by storeId (required)
            predicates.add(criteriaBuilder.equal(root.get("storeId"), storeId.getValue()));

            // Filter by price range if provided (using SKU prices)
            if (priceRange != null) {
                var skuJoin = root.join("skus");
                predicates.add(criteriaBuilder.isNull(skuJoin.get("removedAt")));

                if (priceRange.getFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            skuJoin.get("price"),
                            priceRange.getFrom().getAmount()
                    ));
                }

                if (priceRange.getTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            skuJoin.get("price"),
                            priceRange.getTo().getAmount()
                    ));
                }

                // Ensure we only get distinct products when joining skus
                query.distinct(true);
            }

            // Leave out products already in the collection (picker for "add products")
            if (excludeCollectionId != null) {
                Subquery<Long> inCollection = query.subquery(Long.class);
                Root<ProductCollectionEntity> link = inCollection.from(ProductCollectionEntity.class);
                inCollection.select(link.get("id")).where(
                        criteriaBuilder.equal(link.get("collection").get("id"), excludeCollectionId.getValue()),
                        criteriaBuilder.equal(link.get("productId"), root.get("id")));
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(inCollection)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
