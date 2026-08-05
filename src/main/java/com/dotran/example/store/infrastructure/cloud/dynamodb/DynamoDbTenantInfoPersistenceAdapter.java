package com.dotran.example.store.infrastructure.cloud.dynamodb;

import com.dotran.example.store.application.repository.TenantRepository;
import com.dotran.example.store.common.annotation.PersistenceAdapter;
import com.dotran.example.store.common.domain.valueobject.TenantId;
import com.dotran.example.store.domain.model.TenantInfo;
import com.dotran.example.store.infrastructure.mapper.TenantInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class DynamoDbTenantInfoPersistenceAdapter implements TenantRepository {

    private final DynamoDbTable<TenantInfoItem> tenantInfoTable;
    private final TenantInfoMapper tenantInfoMapper;

    @Override
    public Optional<TenantInfo> findByTenantId(TenantId tenantId) {
        try {
            TenantInfoItem key = new TenantInfoItem();
            key.setPk(tenantId.getValue());
            
            TenantInfoItem item = tenantInfoTable.getItem(key);

            if (item == null) {
                log.debug("TenantInfo not found for tenantId: {}", tenantId.getValue());
                return Optional.empty();
            }

            return Optional.of(tenantInfoMapper.mapTenantInfoItem(item));
        } catch (Exception e) {
            log.error("Error fetching tenant info from DynamoDB for tenantId: {}", tenantId.getValue(), e);
            throw new RuntimeException("Failed to fetch tenant info", e);
        }
    }

    @Override
    public void save(TenantInfo tenantInfo) {
        try {
            TenantInfoItem item = tenantInfoMapper.fromTenantInfoToItem(tenantInfo);
            tenantInfoTable.putItem(item);
            log.info("TenantInfo saved successfully for tenantId: {}", tenantInfo.getId().getValue());
        } catch (Exception e) {
            log.error("Error saving tenant info to DynamoDB for tenantId: {}", tenantInfo.getId().getValue(), e);
            throw new RuntimeException("Failed to save tenant info", e);
        }
    }

    @Override
    public void update(TenantInfo tenantInfo) {
        try {
            TenantInfoItem item = tenantInfoMapper.fromTenantInfoToItem(tenantInfo);
            tenantInfoTable.updateItem(item);
            log.info("TenantInfo updated successfully for tenantId: {}", tenantInfo.getId().getValue());
        } catch (Exception e) {
            log.error("Error updating tenant info in DynamoDB for tenantId: {}", tenantInfo.getId().getValue(), e);
            throw new RuntimeException("Failed to update tenant info", e);
        }
    }

    @Override
    public void delete(TenantId tenantId) {
        try {
            TenantInfoItem key = new TenantInfoItem();
            key.setPk(tenantId.getValue());
            tenantInfoTable.deleteItem(key);
            log.info("TenantInfo deleted successfully for tenantId: {}", tenantId.getValue());
        } catch (Exception e) {
            log.error("Error deleting tenant info from DynamoDB for tenantId: {}", tenantId.getValue(), e);
            throw new RuntimeException("Failed to delete tenant info", e);
        }
    }
}
