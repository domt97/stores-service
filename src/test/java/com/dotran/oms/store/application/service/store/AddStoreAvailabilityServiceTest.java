package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.common.constants.Constants;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.repository.MerchantRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddStoreAvailabilityService")
class AddStoreAvailabilityServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreRepository storeRepository;
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

        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), storeId)).thenReturn(true);
        when(storeDataMapper.fromCmdToStoreAvailability(cmd, storeId)).thenReturn(availability);
        when(storeAvailabilityRepository.save(any(StoreAvailability.class))).thenReturn(savedAvailability);
        when(storeDataMapper.toStoreAvailabilityDto(savedAvailability)).thenReturn(expectedDto);

        StoreAvailabilityDto result = addStoreAvailabilityService.add(cmd);

        assertThat(result).isEqualTo(expectedDto);
        verify(storeAvailabilityRepository).save(any(StoreAvailability.class));
    }

    private AddStoreAvailabilityCmd cmdFor(MerchantId merchantId, StoreId storeId) {
        Instant now = Instant.now();
        return AddStoreAvailabilityCmd.builder().merchantId(merchantId).storeId(storeId).type(AvailabilityType.HOLIDAY)
                .startTime(now).endTime(now.plus(1, ChronoUnit.HOURS)).reason("Holiday").build();
    }

    @Test
    @DisplayName("unknown merchant → 404, the store is never checked, nothing mapped or saved")
    void unknownMerchant_404() {
        AddStoreAvailabilityCmd cmd = cmdFor(MerchantId.newMerchantId(), StoreId.newStoreId());
        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(false);

        assertThatThrownBy(() -> addStoreAvailabilityService.add(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_MERCHANT_NOT_FOUND);

        verifyNoInteractions(storeRepository, storeDataMapper, storeAvailabilityRepository);
    }

    @Test
    @DisplayName("store of another merchant (or unknown) → 404, nothing mapped or saved")
    void storeOfAnotherMerchant_404() {
        AddStoreAvailabilityCmd cmd = cmdFor(MerchantId.newMerchantId(), StoreId.newStoreId());
        when(merchantRepository.existsById(cmd.getMerchantId())).thenReturn(true);
        when(storeRepository.existsByMerchantIdAndStoreId(cmd.getMerchantId(), cmd.getStoreId())).thenReturn(false);

        assertThatThrownBy(() -> addStoreAvailabilityService.add(cmd))
                .isInstanceOf(NotFoundException.class).hasMessage(Constants.ERROR_MSG_STORE_NOT_FOUND);

        verifyNoInteractions(storeDataMapper, storeAvailabilityRepository);
    }
}
