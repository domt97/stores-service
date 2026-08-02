package com.dotran.example.store.infrastructure.rest;

import com.dotran.example.store.application.command.CloseStoreCmd;
import com.dotran.example.store.application.command.CreateStoreCmd;
import com.dotran.example.store.application.command.GetStoreCmd;
import com.dotran.example.store.application.command.ReopenStoreCmd;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.usecase.CloseStoreUseCase;
import com.dotran.example.store.application.usecase.CreateStoreUseCase;
import com.dotran.example.store.application.usecase.GetStoreUseCase;
import com.dotran.example.store.application.usecase.ReopenStoreUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.example.store.infrastructure.rest.response.StoreDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@WebAdapter
@RestController
@RequestMapping(value = "/v1/store")
@RequiredArgsConstructor
@Slf4j
public class StoreController {

    private final StoreRestMapper storeRestMapper;
    private final CreateStoreUseCase createStoreUseCase;
    private final GetStoreUseCase getStoreUseCase;
    private final CloseStoreUseCase closeStoreUseCase;
    private final ReopenStoreUseCase reopenStoreUseCase;

    @PostMapping
    public StoreDetailResponse createStore(@RequestBody CreateStoreRequest createStoreRequest) {
        CreateStoreCmd createStoreCmd = storeRestMapper.fromRequestToCmd(createStoreRequest);

        StoreDetailDto storeDetailDto = createStoreUseCase.create(createStoreCmd);

        return new StoreDetailResponse(storeDetailDto);
    }

    @GetMapping("/{tenantId}/{id}")
    public StoreDetailResponse getStore(@PathVariable UUID tenantId, @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = getStoreUseCase.getStoreByTenantIdAndStoreId(new GetStoreCmd(tenantId, id));

        return new StoreDetailResponse(storeDetailDto);
    }

    @PutMapping("/{tenantId}/{id}/close")
    public StoreDetailResponse closeStore(@PathVariable UUID tenantId, @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = closeStoreUseCase.close(new CloseStoreCmd(tenantId, id));

        return new StoreDetailResponse(storeDetailDto);
    }

    @PutMapping("/{tenantId}/{id}/reopen")
    public StoreDetailResponse reopenStore(@PathVariable UUID tenantId, @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = reopenStoreUseCase.reopen(new ReopenStoreCmd(tenantId, id));

        return new StoreDetailResponse(storeDetailDto);
    }
}
