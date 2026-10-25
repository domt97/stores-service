package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.oms.store.application.dto.StoreAvailabilityDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddStoreAvailabilityService")
class AddStoreAvailabilityServiceTest {

    @Mock private StoreAvailabilityRepository storeAvailabilityRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private AddStoreAvailabilityService addStoreAvailabilityService;

    @Test
    @DisplayName("maps command, initializes, saves and returns DTO")
    void addsAvailability_andReturnsDto() {
        Instant now = Instant.now();
        StoreId storeId = StoreId.newStoreId();
        AddStoreAvailabilityCmd cmd = AddStoreAvailabilityCmd.builder()
                .merchantId(MerchantId.newMerchantId())
                .storeId(storeId)
                .type(AvailabilityType.HOLIDAY)
                .startTime(now)
                .endTime(now.plus(2, ChronoUnit.HOURS))
                .reason("Holiday")
                .build();

        StoreAvailability availability = StoreAvailability.builder()
                .storeId(storeId)
                .type(AvailabilityType.HOLIDAY)
                .startTime(now)
                .endTime(now.plus(2, ChronoUnit.HOURS))
                .reason("Holiday")
                .cancelled(false)
                .build();

        StoreAvailability savedAvailability = StoreAvailability.builder()
                .id(StoreAvailabilityId.generateId())
                .storeId(storeId)
                .type(AvailabilityType.HOLIDAY)
                .startTime(now)
                .endTime(now.plus(2, ChronoUnit.HOURS))
                .reason("Holiday")
                .cancelled(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        StoreAvailabilityDto expectedDto = StoreAvailabilityDto.builder()
                .id(UUID.randomUUID())
                .storeId(storeId.getValue())
                .build();

        when(storeDataMapper.fromCmdToStoreAvailability(cmd, storeId)).thenReturn(availability);
        when(storeAvailabilityRepository.save(any(StoreAvailability.class))).thenReturn(savedAvailability);
        when(storeDataMapper.toStoreAvailabilityDto(savedAvailability)).thenReturn(expectedDto);

        StoreAvailabilityDto result = addStoreAvailabilityService.add(cmd);

        assertThat(result).isEqualTo(expectedDto);
        verify(storeAvailabilityRepository).save(any(StoreAvailability.class));
    }
}
