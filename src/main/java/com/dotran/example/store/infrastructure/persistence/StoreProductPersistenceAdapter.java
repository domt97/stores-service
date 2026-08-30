package com.dotran.example.store.infrastructure.persistence;

import com.dotran.example.store.application.repository.StoreProductRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.common.domain.valueobject.PriceRange;
import com.dotran.example.store.common.domain.valueobject.ProductId;
import com.dotran.example.store.common.domain.valueobject.StoreId;
import com.dotran.example.store.common.dto.DomainPageRequest;
import com.dotran.example.store.common.dto.PagedResult;
import com.dotran.example.store.common.exception.ValidationException;
import com.dotran.example.store.common.utils.CollectionUtils;
import com.dotran.example.store.common.utils.PageUtils;
import com.dotran.example.store.domain.model.StoreProduct;
import com.dotran.example.store.infrastructure.mapper.StoreProductPersistenceMapper;
import com.dotran.example.store.infrastructure.persistence.entity.StoreProductEntity;
import com.dotran.example.store.infrastructure.persistence.jpa.SpringDataStoreProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StoreProductPersistenceAdapter implements StoreProductRepository {

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
    public List<StoreProduct> getProductsByListOfProductIds(List<ProductId> productIds) {
        if (null == productIds || productIds.isEmpty()) {
            return new ArrayList<>();
        }

        return repository.findAllByIdIn(productIds.stream().map(ProductId::getValue).toList())
                .stream()
                .map(mapper::fromEntity)
                .toList();
    }

    @Override
    public PagedResult<StoreProduct> searchProducts(StoreId storeId,
                                                    PriceRange priceRange,
                                                    DomainPageRequest pageRequest) {
        Specification<StoreProductEntity> specification = buildSearchSpecification(storeId, priceRange);

        Page<StoreProduct> storeProductPage = repository.findAll(specification, PageUtils.toPageRequest(pageRequest))
                .map(mapper::fromEntity);

        return PagedResult.of(storeProductPage);
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
     * @return JPA Specification
     */
    private Specification<StoreProductEntity> buildSearchSpecification(StoreId storeId, PriceRange priceRange) {
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

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
