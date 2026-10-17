package com.dotran.oms.store.application.service.store;

import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import com.dotran.oms.core.exception.NotFoundException;
import com.dotran.oms.store.application.command.store.CreateStoreCmd;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.mapper.StoreDataMapper;
import com.dotran.oms.store.application.repository.StoreRepository;
import com.dotran.oms.store.application.repository.TenantRepository;
import com.dotran.oms.store.domain.enums.StoreStatus;
import com.dotran.oms.store.domain.model.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateStoreService")
class CreateStoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private StoreDataMapper storeDataMapper;

    @InjectMocks
    private CreateStoreService createStoreService;

    @Test
    @DisplayName("throws NotFoundException when the tenant does not exist")
    void throwsException_whenTenantNotFound() {
        TenantId tenantId = TenantId.newTenantId();
        CreateStoreCmd cmd = CreateStoreCmd.builder()
                .tenantId(tenantId)
                .name("My Store")
                .code("STR001")
                .build();

        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createStoreService.create(cmd))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("creates a store and returns its DTO when tenant exists")
    void createsStore_andReturnsDto_whenTenantFound() {
        TenantId tenantId = TenantId.newTenantId();
        TenantId resolvedTenantId = TenantId.newTenantId();

        TenantInfo tenantInfo = TenantInfo.builder()
                .id(resolvedTenantId)
                .code("ACME")
                .name("ACME Corp")
                .status("ACTIVE")
                .build();

        CreateStoreCmd cmd = CreateStoreCmd.builder()
                .tenantId(tenantId)
                .name("My Store")
                .code("STR001")
                .ownerId(CustomerId.newCustomerId())
                .email("store@test.com")
                .phone("0123456789")
                .build();

        Store savedStore = Store.builder()
                .id(StoreId.newStoreId())
                .tenantId(resolvedTenantId)
                .name("My Store")
                .code("STR001")
                .status(StoreStatus.ACTIVE)
                .businessHours(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StoreDetailDto expectedDto = StoreDetailDto.builder()
                .id(UUID.randomUUID())
                .name("My Store")
                .status(StoreStatus.ACTIVE)
                .build();

        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenantInfo));
        when(storeRepository.create(any(Store.class))).thenReturn(savedStore);
        when(storeDataMapper.toStoreDetailDto(savedStore)).thenReturn(expectedDto);

        StoreDetailDto result = createStoreService.create(cmd);

        assertThat(result).isEqualTo(expectedDto);
        verify(storeRepository).create(any(Store.class));
    }
}
