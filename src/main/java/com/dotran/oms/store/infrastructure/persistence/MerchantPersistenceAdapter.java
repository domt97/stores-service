package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.domain.exception.MerchantCodeAlreadyExistsException;
import com.dotran.oms.store.domain.model.Merchant;
import com.dotran.oms.store.infrastructure.mapper.MerchantPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.MerchantEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class MerchantPersistenceAdapter implements MerchantRepository {

    static final String UNIQUE_CODE_CONSTRAINT = "uk_merchants_code";

    private final SpringDataMerchantRepository springDataMerchantRepository;
    private final MerchantPersistenceMapper merchantPersistenceMapper;

    @Override
    public Merchant create(Merchant merchant) {
        MerchantEntity entity = merchantPersistenceMapper.fromMerchantToEntity(merchant);
        try {
            MerchantEntity saved = springDataMerchantRepository.saveAndFlush(entity);
            return merchantPersistenceMapper.fromEntityToMerchant(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueCodeViolation(ex)) {
                // A concurrent registration won the race; the transaction is rolled back by the caller.
                throw new MerchantCodeAlreadyExistsException();
            }
            throw ex;
        }
    }

    @Override
    public Optional<Merchant> findById(MerchantId merchantId) {
        return springDataMerchantRepository.findById(merchantId.getValue())
                .map(merchantPersistenceMapper::fromEntityToMerchant);
    }

    @Override
    public boolean existsById(MerchantId merchantId) {
        return springDataMerchantRepository.existsById(merchantId.getValue());
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataMerchantRepository.existsByCode(code);
    }

    private static boolean isUniqueCodeViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();
        return message != null && message.contains(UNIQUE_CODE_CONSTRAINT);
    }
}
