package com.dotran.example.store.application.client.inventory;

import java.util.List;

public interface InventoryServiceClient {

    ItemDetailDto checkItemDetail(List<ItemDetailCheck> itemDetailCheck);
}
