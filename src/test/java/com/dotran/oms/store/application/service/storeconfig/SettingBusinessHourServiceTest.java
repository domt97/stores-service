package com.dotran.oms.store.application.service.storeconfig;

import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.core.exception.BusinessException;
import com.dotran.oms.store.application.command.storeconfig.UpdateBusinessHourCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.exception.StoreNotFoundException;
import com.dotran.oms.store.domain.model.BusinessHour;
import com.dotran.oms.store.domain.model.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingBusinessHourService")
class SettingBusinessHourServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private SettingBusinessHourService settingBusinessHourService;

    private final MerchantId merchantId = MerchantId.newMerchantId();
    private final StoreId storeId = StoreId.newStoreId();

    private Store storeWithDefaultBusinessHours() {
        return Store.builder()
                .id(storeId)
                .merchantId(merchantId)
                .name("Test Store")
                .status(StoreStatus.ACTIVE)
                .businessHours(BusinessHour.defaultConfig())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private List<UpdateBusinessHourCmd> cmdsForAllDays() {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> UpdateBusinessHourCmd.builder()
                        .dayOfWeek(day)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(18, 0))
                        .closed(false)
                        .build())
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("throws StoreNotFoundException when the store does not exist")
    void throwsException_whenStoreNotFound() {
        when(storeRepository.findByMerchantIdAndStoreId(any(MerchantId.class), any(StoreId.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingBusinessHourService
                .setupBusinessHour(merchantId.getValue(), storeId.getValue(), cmdsForAllDays()))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    @DisplayName("throws BusinessException when a day from the store's hours is missing from the command")
    void throwsException_whenMissingDayConfig() {
        Store store = storeWithDefaultBusinessHours(); // has all 7 days
        when(storeRepository.findByMerchantIdAndStoreId(any(MerchantId.class), any(StoreId.class)))
                .thenReturn(Optional.of(store));

        // Omit SUNDAY from the commands
        List<UpdateBusinessHourCmd> incompleteCmds = Arrays.stream(DayOfWeek.values())
                .filter(day -> day != DayOfWeek.SUNDAY)
                .map(day -> UpdateBusinessHourCmd.builder()
                        .dayOfWeek(day)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(18, 0))
                        .closed(false)
                        .build())
                .collect(Collectors.toList());

        assertThatThrownBy(() -> settingBusinessHourService
                .setupBusinessHour(merchantId.getValue(), storeId.getValue(), incompleteCmds))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("updates business hours and returns the DTO when all days are provided")
    void updatesBusinessHours_andReturnsDto() {
        Store store = storeWithDefaultBusinessHours();

        Store updatedStore = storeWithDefaultBusinessHours();
        StoreDetailDto expectedDto = StoreDetailDto.builder()
                .id(UUID.randomUUID())
                .status(StoreStatus.ACTIVE)
                .build();

        when(storeRepository.findByMerchantIdAndStoreId(any(MerchantId.class), any(StoreId.class)))
                .thenReturn(Optional.of(store));
        when(storeRepository.update(any(Store.class))).thenReturn(updatedStore);
        when(storeDataMapper.toStoreDetailDto(updatedStore)).thenReturn(expectedDto);

        StoreDetailDto result = settingBusinessHourService
                .setupBusinessHour(merchantId.getValue(), storeId.getValue(), cmdsForAllDays());

        assertThat(result).isEqualTo(expectedDto);
    }
}
