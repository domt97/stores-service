package com.dotran.example.store.infrastructure.rest.api;

import com.dotran.example.store.application.command.AddStoreAvailabilityCmd;
import com.dotran.example.store.application.command.CloseStoreCmd;
import com.dotran.example.store.application.command.CreateStoreCmd;
import com.dotran.example.store.application.command.GetStoreCmd;
import com.dotran.example.store.application.command.ReopenStoreCmd;
import com.dotran.example.store.application.dto.StoreAvailabilityDto;
import com.dotran.example.store.application.dto.StoreDetailDto;
import com.dotran.example.store.application.usecase.AddStoreAvailabilityUseCase;
import com.dotran.example.store.application.usecase.CancelStoreAvailabilityUseCase;
import com.dotran.example.store.application.usecase.CloseStoreUseCase;
import com.dotran.example.store.application.usecase.CreateStoreUseCase;
import com.dotran.example.store.application.usecase.GetStoreUseCase;
import com.dotran.example.store.application.usecase.ReopenStoreUseCase;
import com.dotran.example.store.common.annotation.WebAdapter;
import com.dotran.example.store.infrastructure.rest.dto.request.AddStoreAvailabilityRequest;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.example.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.example.store.infrastructure.rest.response.StoreAvailabilityResponse;
import com.dotran.example.store.infrastructure.rest.response.StoreDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final AddStoreAvailabilityUseCase addStoreAvailabilityUseCase;
    private final CancelStoreAvailabilityUseCase cancelStoreAvailabilityUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreDetailResponse createStore(@RequestBody @Valid CreateStoreRequest createStoreRequest) {
        CreateStoreCmd createStoreCmd = storeRestMapper.fromRequestToCmd(createStoreRequest);
        StoreDetailDto storeDetailDto = createStoreUseCase.create(createStoreCmd);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @GetMapping("/{tenantId}/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StoreDetailResponse getStore(@PathVariable UUID tenantId, @PathVariable UUID id) {
        log.info("StoreController - getStore: START");
        StoreDetailDto storeDetailDto = getStoreUseCase.getStoreByTenantIdAndStoreId(new GetStoreCmd(tenantId, id));
        log.info("StoreController - getStore: END");
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @PutMapping("/{tenantId}/{id}/close")
    @ResponseStatus(HttpStatus.OK)
    public StoreDetailResponse closeStore(@PathVariable UUID tenantId, @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = closeStoreUseCase.close(new CloseStoreCmd(tenantId, id));
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @PutMapping("/{tenantId}/{id}/reopen")
    @ResponseStatus(HttpStatus.OK)
    public StoreDetailResponse reopenStore(@PathVariable UUID tenantId, @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = reopenStoreUseCase.reopen(new ReopenStoreCmd(tenantId, id));
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @PostMapping("/{tenantId}/{id}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreAvailabilityResponse addStoreAvailability(
            @PathVariable UUID tenantId,
            @PathVariable UUID id,
            @RequestBody @Valid AddStoreAvailabilityRequest request) {
        log.info("StoreController - addStoreAvailability: START for storeId={}", id);

        AddStoreAvailabilityCmd cmd = storeRestMapper.fromRequestToAddStoreAvailabilityCmd(request);
        cmd.setTenantId(tenantId);
        cmd.setStoreId(id);

        StoreAvailabilityDto storeAvailabilityDto = addStoreAvailabilityUseCase.add(cmd);

        log.info("StoreController - addStoreAvailability: END");
        return storeRestMapper.toStoreAvailabilityResponse(storeAvailabilityDto);
    }

    @PostMapping("/{tenantId}/{id}/availability/{storeAvailabilityId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelStoreAvailability(
            @PathVariable UUID tenantId,
            @PathVariable UUID id,
            @PathVariable UUID storeAvailabilityId) {
        log.info("StoreController - cancelStoreAvailability: START for storeId={}", id);
        cancelStoreAvailabilityUseCase.cancel(storeAvailabilityId, id);
        log.info("StoreController - cancelStoreAvailability: END");
    }
}
