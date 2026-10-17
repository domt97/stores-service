package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.repository.StoreAvailabilityRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelStoreAvailabilityService")
class CancelStoreAvailabilityServiceTest {

    @Mock private StoreAvailabilityRepository storeAvailabilityRepository;

    @InjectMocks
    private CancelStoreAvailabilityService cancelStoreAvailabilityService;

    @Test
    @DisplayName("throws NotFoundException when availability does not exist")
    void throwsException_whenNotFound() {
        UUID availabilityId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        when(storeAvailabilityRepository.findByIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancelStoreAvailabilityService.cancel(availabilityId, storeId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("cancels the availability and saves it when found")
    void cancelsAvailability_whenFound() {
        UUID availabilityId = UUID.randomUUID();
        UUID storeIdValue = UUID.randomUUID();
        Instant now = Instant.now();

        StoreAvailability availability = StoreAvailability.builder()
                .id(StoreAvailabilityId.of(availabilityId))
                .type(AvailabilityType.HOLIDAY)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now.plus(1, ChronoUnit.HOURS))
                .cancelled(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(storeAvailabilityRepository.findByIdAndStoreId(any(), any())).thenReturn(Optional.of(availability));
        when(storeAvailabilityRepository.save(any(StoreAvailability.class))).thenReturn(availability);

        cancelStoreAvailabilityService.cancel(availabilityId, storeIdValue);

        verify(storeAvailabilityRepository).save(availability);
    }
}
