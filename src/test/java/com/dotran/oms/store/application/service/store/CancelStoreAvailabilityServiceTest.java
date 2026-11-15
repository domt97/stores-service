package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.store.CancelStoreAvailabilityCmd;
import com.dotran.oms.store.application.repository.MerchantRepository;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.common.id.StoreAvailabilityId;
import com.dotran.oms.store.domain.enums.AvailabilityType;
import com.dotran.oms.store.domain.model.StoreAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelStoreAvailabilityService")
class CancelStoreAvailabilityServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreAvailabilityRepository storeAvailabilityRepository;

    @InjectMocks
    private CancelStoreAvailabilityService service;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();
    private final StoreAvailabilityId availabilityId = StoreAvailabilityId.generateId();
    private final CancelStoreAvailabilityCmd cmd = new CancelStoreAvailabilityCmd(merchantId, storeId, availabilityId);

    private StoreAvailability window(boolean cancelled) {
        Instant now = Instant.now();
        return StoreAvailability.builder()
                .id(availabilityId)
                .storeId(storeId)
                .type(AvailabilityType.HOLIDAY)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now.plus(1, ChronoUnit.HOURS))
                .cancelled(cancelled)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void merchantOwnsStore() {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(true);
    }

    @Test
    @DisplayName("the merchant's store and its window → cancelled and saved")
    void cancels() {
        merchantOwnsStore();
        StoreAvailability window = window(false);
        when(storeAvailabilityRepository.findByIdAndStoreId(availabilityId, storeId)).thenReturn(Optional.of(window));

        service.cancel(cmd);

        assertThat(window.isCancelled()).isTrue();
        verify(storeAvailabilityRepository).save(window);
    }

    @Test
    @DisplayName("unknown merchant → 404, the store and the window are never read, nothing saved")
    void unknownMerchant_404() {
        when(merchantRepository.existsById(merchantId)).thenReturn(false);

        assertThatThrownBy(() -> service.cancel(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);

        verifyNoInteractions(storeRepository, storeAvailabilityRepository);
    }

    @Test
    @DisplayName("store of another merchant → 404, the window is never read, nothing saved")
    void storeOfAnotherMerchant_404() {
        when(merchantRepository.existsById(merchantId)).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(merchantId, storeId)).thenReturn(false);

        assertThatThrownBy(() -> service.cancel(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);

        verifyNoInteractions(storeAvailabilityRepository);
    }

    @Test
    @DisplayName("a window of another store (not found for this store) → 404, nothing saved")
    void windowOfAnotherStore_404() {
        merchantOwnsStore();
        when(storeAvailabilityRepository.findByIdAndStoreId(availabilityId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_STORE_AVAILABILITY_NOT_FOUND);

        verify(storeAvailabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("an already cancelled window → 422 as before, nothing saved")
    void alreadyCancelled_422() {
        merchantOwnsStore();
        when(storeAvailabilityRepository.findByIdAndStoreId(availabilityId, storeId)).thenReturn(Optional.of(window(true)));

        assertThatThrownBy(() -> service.cancel(cmd)).isInstanceOf(BusinessException.class)
                .hasMessage("This Store Availability config is already cancelled");

        verify(storeAvailabilityRepository, never()).save(any());
    }
}
