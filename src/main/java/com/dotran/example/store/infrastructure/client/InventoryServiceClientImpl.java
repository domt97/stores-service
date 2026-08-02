package com.dotran.example.store.infrastructure.client;

import com.dotran.example.store.application.client.inventory.InventoryServiceClient;
import com.dotran.example.store.application.client.inventory.ItemDetailCheck;
import com.dotran.example.store.application.client.inventory.ItemDetailDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryServiceClientImpl implements InventoryServiceClient {

    @Override
    public ItemDetailDto checkItemDetail(List<ItemDetailCheck> itemDetailCheck) {
        return null;
    }
}
