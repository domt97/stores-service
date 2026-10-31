package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.core.annotation.WebAdapter;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.command.store.AddStoreAvailabilityCmd;
import com.dotran.oms.store.application.command.store.CreateStoreCmd;
import com.dotran.oms.store.application.command.store.GetStoreCmd;
import com.dotran.oms.store.application.command.store.ReopenStoreCmd;
import com.dotran.oms.store.application.dto.StoreAvailabilityDto;
import com.dotran.oms.store.application.dto.StoreDetailDto;
import com.dotran.oms.store.application.usecase.store.AddStoreAvailabilityUseCase;
import com.dotran.oms.store.application.usecase.store.CancelStoreAvailabilityUseCase;
import com.dotran.oms.store.application.usecase.store.CloseStoreUseCase;
import com.dotran.oms.store.application.usecase.store.CreateStoreUseCase;
import com.dotran.oms.store.application.usecase.store.GetStoreUseCase;
import com.dotran.oms.store.application.usecase.store.ReopenStoreUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.AddStoreAvailabilityRequest;
import com.dotran.oms.store.infrastructure.rest.dto.request.CreateStoreRequest;
import com.dotran.oms.store.infrastructure.rest.mapper.StoreRestMapper;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreAvailabilityResponse;
import com.dotran.oms.store.infrastructure.rest.dto.response.StoreDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Store Management", description = "APIs for managing store lifecycle, availability and operations")
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

    @Operation(summary = "Create a new store", description = "Creates a new store for the specified merchant with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Store created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Merchant not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Store already exists", content = @Content)
    })
    @PostMapping("/merchants/{merchantId}/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreDetailResponse createStore(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @RequestBody @Valid CreateStoreRequest createStoreRequest) {
        CreateStoreCmd createStoreCmd = storeRestMapper.fromRequestToCmd(createStoreRequest);
        createStoreCmd.setMerchantId(MerchantId.of(merchantId));
        StoreDetailDto storeDetailDto = createStoreUseCase.create(createStoreCmd);
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @Operation(summary = "Get store details", description = "Retrieves detailed information about a specific store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @GetMapping("/merchants/{merchantId}/stores/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StoreDetailResponse getStore(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID id) {
        log.info("StoreController - getStore: START");
        StoreDetailDto storeDetailDto = getStoreUseCase.getStoreByMerchantIdAndStoreId(
                GetStoreCmd.builder()
                        .merchantId(MerchantId.of(merchantId))
                        .storeId(StoreId.of(id))
                        .build());
        log.info("StoreController - getStore: END");
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @Operation(summary = "Close a store", description = "Closes an active store, preventing new orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store closed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Store is already closed", content = @Content)
    })
    @PutMapping("/merchants/{merchantId}/stores/{id}/close")
    @ResponseStatus(HttpStatus.OK)
    public StoreDetailResponse closeStore(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = closeStoreUseCase.close(storeRestMapper.fromRequestToCloseStoreCmd(merchantId, id));
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @Operation(summary = "Reopen a store", description = "Reopens a closed store, allowing new orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store reopened successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Store is already open", content = @Content)
    })
    @PutMapping("/merchants/{merchantId}/stores/{id}/reopen")
    @ResponseStatus(HttpStatus.OK)
    public StoreDetailResponse reopenStore(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID id) {
        StoreDetailDto storeDetailDto = reopenStoreUseCase.reopen(
                ReopenStoreCmd.builder()
                        .merchantId(MerchantId.of(merchantId))
                        .storeId(StoreId.of(id))
                        .build()
        );
        return storeRestMapper.toStoreDetailResponse(storeDetailDto);
    }

    @Operation(summary = "Add store availability", description = "Adds a new availability schedule for the store")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Availability added successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreAvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @PostMapping("/merchants/{merchantId}/stores/{id}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreAvailabilityResponse addStoreAvailability(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID id,
            @RequestBody @Valid AddStoreAvailabilityRequest request) {
        log.info("StoreController - addStoreAvailability: START for storeId={}", id);

        AddStoreAvailabilityCmd cmd = storeRestMapper.fromRequestToAddStoreAvailabilityCmd(request);
        cmd.setMerchantId(MerchantId.of(merchantId));
        cmd.setStoreId(StoreId.of(id));

        StoreAvailabilityDto storeAvailabilityDto = addStoreAvailabilityUseCase.add(cmd);

        log.info("StoreController - addStoreAvailability: END");
        return storeRestMapper.toStoreAvailabilityResponse(storeAvailabilityDto);
    }

    @Operation(summary = "Cancel store availability", description = "Cancels an existing availability schedule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Availability cancelled successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store or availability not found", content = @Content)
    })
    @PostMapping("/merchants/{merchantId}/stores/{id}/availability/{storeAvailabilityId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelStoreAvailability(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId,
            @Parameter(description = "Store ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Store Availability ID", required = true) @PathVariable UUID storeAvailabilityId) {
        log.info("StoreController - cancelStoreAvailability: START for storeId={}", id);
        cancelStoreAvailabilityUseCase.cancel(storeAvailabilityId, id);
        log.info("StoreController - cancelStoreAvailability: END");
    }
}
