package com.dotran.oms.store.infrastructure.persistence;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.domain.exception.MerchantCodeAlreadyExistsException;
import com.dotran.oms.store.domain.model.Merchant;
import com.dotran.oms.store.infrastructure.mapper.MerchantPersistenceMapper;
import com.dotran.oms.store.infrastructure.persistence.entity.MerchantEntity;
import com.dotran.oms.store.infrastructure.persistence.jpa.SpringDataMerchantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantPersistenceAdapter")
class MerchantPersistenceAdapterTest {

    @Mock private SpringDataMerchantRepository springDataMerchantRepository;
    @Mock private MerchantPersistenceMapper merchantPersistenceMapper;

    @InjectMocks
    private MerchantPersistenceAdapter adapter;

    @Test
    @DisplayName("create: maps, saves and flushes, returns the persisted merchant")
    void create_savesAndReturnsDomain() {
        Merchant merchant = Merchant.register("M001", "Tea");
        MerchantEntity entity = mock(MerchantEntity.class);
        MerchantEntity saved = mock(MerchantEntity.class);
        Merchant persisted = mock(Merchant.class);
        when(merchantPersistenceMapper.fromMerchantToEntity(merchant)).thenReturn(entity);
        when(springDataMerchantRepository.saveAndFlush(entity)).thenReturn(saved);
        when(merchantPersistenceMapper.fromEntityToMerchant(saved)).thenReturn(persisted);

        assertThat(adapter.create(merchant)).isEqualTo(persisted);
    }

    @Test
    @DisplayName("create: unique code violation (concurrent registration) → MerchantCodeAlreadyExistsException")
    void create_translatesUniqueCodeViolation() {
        Merchant merchant = Merchant.register("M001", "Tea");
        MerchantEntity entity = mock(MerchantEntity.class);
        when(merchantPersistenceMapper.fromMerchantToEntity(merchant)).thenReturn(entity);
        when(springDataMerchantRepository.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"uk_merchants_code\"")));

        assertThatThrownBy(() -> adapter.create(merchant)).isInstanceOf(MerchantCodeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("create: other integrity violations are rethrown unchanged")
    void create_rethrowsOtherViolations() {
        Merchant merchant = Merchant.register("M001", "Tea");
        MerchantEntity entity = mock(MerchantEntity.class);
        DataIntegrityViolationException other = new DataIntegrityViolationException(
                "could not execute statement", new SQLException("null value in column \"name\""));
        when(merchantPersistenceMapper.fromMerchantToEntity(merchant)).thenReturn(entity);
        when(springDataMerchantRepository.saveAndFlush(entity)).thenThrow(other);

        assertThatThrownBy(() -> adapter.create(merchant)).isSameAs(other);
    }

    @Test
    @DisplayName("create: a code CHECK violation is not mistaken for a duplicate code")
    void create_rethrowsCodeCheckViolation() {
        Merchant merchant = Merchant.register("M001", "Tea");
        MerchantEntity entity = mock(MerchantEntity.class);
        DataIntegrityViolationException check = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("new row for relation \"merchants\" violates check constraint \"ck_merchants_code\""));
        when(merchantPersistenceMapper.fromMerchantToEntity(merchant)).thenReturn(entity);
        when(springDataMerchantRepository.saveAndFlush(entity)).thenThrow(check);

        assertThatThrownBy(() -> adapter.create(merchant)).isSameAs(check);
    }

    @Test
    @DisplayName("findById / existsById / existsByCode delegate with the raw ids")
    void lookupsDelegate() {
        MerchantId merchantId = MerchantId.newMerchantId();
        MerchantEntity entity = mock(MerchantEntity.class);
        Merchant merchant = mock(Merchant.class);
        when(springDataMerchantRepository.findById(merchantId.getValue())).thenReturn(Optional.of(entity));
        when(merchantPersistenceMapper.fromEntityToMerchant(entity)).thenReturn(merchant);
        when(springDataMerchantRepository.existsById(merchantId.getValue())).thenReturn(true);
        when(springDataMerchantRepository.existsByCode("M001")).thenReturn(false);

        assertThat(adapter.findById(merchantId)).contains(merchant);
        assertThat(adapter.existsById(merchantId)).isTrue();
        assertThat(adapter.existsByCode("M001")).isFalse();
    }
}
