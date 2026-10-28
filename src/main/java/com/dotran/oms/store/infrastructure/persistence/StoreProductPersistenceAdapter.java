package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
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

import java.util.ArrayList;
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

    private static final String ID_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "name", ID_FIELD);

    private final SpringDataStoreProductRepository repository;
    private final StoreProductPersistenceMapper mapper;

    @Override
    public StoreProduct create(StoreProduct storeProduct) {
        StoreProductEntity storeProductEntity = mapper.fromStoreProduct(storeProduct);
        storeProductEntity.setSkus(mapper.fromProductSkus(storeProduct.getSkus(), storeProductEntity));
        storeProductEntity.setImages(mapper.fromProductImages(storeProduct.getImages(), storeProductEntity));

        StoreProductEntity savedEntity = repository.saveAndFlush(storeProductEntity);

        return mapper.fromEntity(savedEntity);
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
            StoreProductEntity storeProductEntity = mapper.fromStoreProduct(storeProduct);
            storeProductEntity.setSkus(mapper.fromProductSkus(storeProduct.getSkus(), storeProductEntity));
            storeProductEntity.setImages(mapper.fromProductImages(storeProduct.getImages(), storeProductEntity));

            storeProductEntityList.add(storeProductEntity);
        }
        return repository.saveAllAndFlush(storeProductEntityList)
                .stream()
                .map(mapper::fromEntity)
                .toList();
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
